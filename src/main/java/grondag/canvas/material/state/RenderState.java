/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.material.state;

import static grondag.canvas.material.state.MaterialStateEncoder.R_BLUR;
import static grondag.canvas.material.state.MaterialStateEncoder.R_CULL;
import static grondag.canvas.material.state.MaterialStateEncoder.R_DECAL;
import static grondag.canvas.material.state.MaterialStateEncoder.R_DEPTH_TEST;
import static grondag.canvas.material.state.MaterialStateEncoder.R_LINES;
import static grondag.canvas.material.state.MaterialStateEncoder.R_TARGET;
import static grondag.canvas.material.state.MaterialStateEncoder.R_TEXTURE;
import static grondag.canvas.material.state.MaterialStateEncoder.R_TRANSPARENCY;
import static grondag.canvas.material.state.MaterialStateEncoder.R_WRITE_MASK;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;

import io.vram.bitkit.BitPacker64;
import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.renderer.MaterialTextureManager;
import io.vram.frex.api.texture.MaterialTexture;

import grondag.canvas.config.Configurator;
import grondag.canvas.light.color.LightDataManager;
import grondag.canvas.light.color.LightDataTexture;
import grondag.canvas.material.property.BinaryRenderState;
import grondag.canvas.material.property.DecalRenderState;
import grondag.canvas.material.property.DepthTestRenderState;
import grondag.canvas.material.property.TargetRenderState;
import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.material.property.TransparencyRenderState;
import grondag.canvas.material.property.WriteMaskRenderState;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.render.world.SkyShadowRenderer;
import grondag.canvas.shader.MaterialProgram;
import grondag.canvas.shader.ProgramType;
import grondag.canvas.shader.data.MatrixState;
import grondag.canvas.texture.MaterialIndexTexture;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

/**
 * Primitives with the same state have the same vertex encoding,
 * same uniform state and same GL draw state. Analogous to RenderLayer.
 *
 * <p>Also serves as the key for vertex collection. Primitives with the same state
 * can share the same draw call and should be packed contiguously in the buffer.
 *
 * <p>Primitives must have the same sorting requirements, which for all but the translucent
 * collection keys means there is no sorting. Translucent primitives that require sorting
 * all belong to a small handful of collectors.
 *
 * <p>Vertex data with different state can share the same buffer and should be
 * packed in glState, uniformState order for best performance.
 */
public final class RenderState {
	protected final long bits;
	public final int index;

	public final TargetRenderState target;
	public final MaterialTexture materialTexture;
	public final TextureMaterialState texture;
	public final boolean blur;
	public final TransparencyRenderState transparency;
	public final DepthTestRenderState depthTest;
	public final boolean cull;
	public final WriteMaskRenderState writeMask;
	public final DecalRenderState decal;
	public final boolean lines;

	public final boolean primaryTargetTransparency;
	public final long drawPriority;

	protected RenderState(long bits) {
		this.bits = bits;
		this.index = nextIndex++;
		target = TargetRenderState.fromIndex(R_TARGET.getValue(bits));
		materialTexture = textures.textureFromIndex(R_TEXTURE.getValue(bits));
		texture = TextureMaterialState.fromId(materialTexture.id());
		blur = R_BLUR.getValue(bits);
		transparency = TransparencyRenderState.fromIndex(R_TRANSPARENCY.getValue(bits));
		depthTest = DepthTestRenderState.fromIndex(R_DEPTH_TEST.getValue(bits));
		cull = R_CULL.getValue(bits);
		writeMask = WriteMaskRenderState.fromIndex(R_WRITE_MASK.getValue(bits));
		decal = DecalRenderState.fromIndex(R_DECAL.getValue(bits));
		lines = R_LINES.getValue(bits);
		//castShadows = !DISABLE_SHADOWS.getValue(bits);
		primaryTargetTransparency = MaterialStateEncoder.primaryTargetTransparency(bits);
		drawPriority = drawPriority();
	}

	private long drawPriority() {
		long result = SORT_BLUR.setValue(blur, 0);
		result = SORT_DEPTH_TEST.setValue(depthTest.index, result);
		result = SORT_CULL.setValue(cull, result);
		result = SORT_LINES.setValue(lines, result);
		result = SORT_DECAL.setValue(decal.drawPriority, result);
		// inverted because higher goes first
		result = SORT_TPP.setValue(!primaryTargetTransparency, result);
		result = SORT_TRANSPARENCY.setValue(transparency.drawPriority, result);
		result = SORT_WRITE_MASK.setValue(writeMask.drawPriority, result);

		return result;
	}

	public void enable() {
		enable(0, 0, 0);
	}

	public void enable(int x, int y, int z) {
		if (SkyShadowRenderer.isActive()) {
			enableDepthPass(x, y, z, SkyShadowRenderer.cascade());
		} else {
			enableMaterial(x, y, z);
		}
	}

	private void enableDepthPass(int x, int y, int z, int cascade) {
		final MatrixState matrixState = MatrixState.get();
		final ProgramType programType = matrixState == MatrixState.REGION ? ProgramType.MATERIAL_DEPTH_TERRAIN : ProgramType.MATERIAL_DEPTH;
		final MaterialProgram depthShader = MaterialProgram.get(programType, target.index);

		if (shadowActive == this && shadowCurrentMatrixState == matrixState) {
			depthShader.setModelOrigin(x, y, z);
			depthShader.setCascade(cascade);
			return;
		}

		if (shadowActive == null) {
			// same for all, so only do 1X
			Pipeline.skyShadowFbo.bind();
		}

		shadowActive = this;
		shadowCurrentMatrixState = matrixState;
		active = null;
		currentMatrixState = null;
		texture.materialIndexProvider().enable();

		texture.enable(blur);
		transparency.enable();
		depthTest.enable();
		writeMask.enable();

		// NB: Could probably disable decal renders in depth pass for most use cases
		// but there's nothing to prevent anyone from rendering stacked cutout decals.
		// Decals aren't super common so left in for now.
		decal.enable();

		CULL_STATE.setEnabled(cull && Configurator.shadowFaceCulling != SkyShadowRenderer.Culling.NONE);
		LIGHTMAP_STATE.setEnabled(true);
		LINE_STATE.setEnabled(lines);

		depthShader.updateContextInfo(texture.spriteIndex(), target.index);
		depthShader.setModelOrigin(x, y, z);
		depthShader.setCascade(cascade);

		GFX.enable(GFX.GL_POLYGON_OFFSET_FILL);
		GFX.polygonOffset(Pipeline.shadowSlopeFactor, Pipeline.shadowBiasUnits);

		switch (Configurator.shadowFaceCulling) {
			case FRONT -> GFX.glCullFace(GFX.GL_FRONT);
			case BACK -> GFX.glCullFace(GFX.GL_BACK);
			case NONE -> { }
		}
	}

	private void enableMaterial(int x, int y, int z) {
		final MatrixState matrixState = MatrixState.get();
		final ProgramType programType = matrixState == MatrixState.REGION ? ProgramType.MATERIAL_COLOR_TERRAIN : ProgramType.MATERIAL_COLOR;
		final MaterialProgram shader = MaterialProgram.get(programType, target.index);

		if (active == this && matrixState == currentMatrixState) {
			shader.setModelOrigin(x, y, z);
			return;
		}

		//		if (enablePrint) {
		//			GlStateSpy.print();
		//		}

		if (active == null || active.target != target) {
			target.enable();
		}

		active = this;
		currentMatrixState = matrixState;
		shadowActive = null;
		shadowCurrentMatrixState = null;
		texture.materialIndexProvider().enable();

		if (Pipeline.shadowMapDepth != -1) {
			CanvasTextureState.ensureTextureOfTextureUnit(TextureData.SHADOWMAP, GFX.GL_TEXTURE_2D_ARRAY, Pipeline.shadowMapDepth);
			CanvasTextureState.ensureTextureOfTextureUnit(TextureData.SHADOWMAP_TEXTURE, GFX.GL_TEXTURE_2D_ARRAY, Pipeline.shadowMapDepth);
		}

		if (Pipeline.coloredLightsEnabled()) {
			CanvasTextureState.ensureTextureOfTextureUnit(TextureData.COLORED_LIGHTS_DATA, LightDataTexture.Format.TARGET, LightDataManager.texId());
		}

		if (Pipeline.config().materialProgram.samplerNames.length > 0) {
			// Activate non-frex material program textures
			for (int i = 0; i < Pipeline.config().materialProgram.samplerNames.length; i++) {
				final int bindTarget = Pipeline.materialTextures().texTargets[i];
				final int bind = Pipeline.materialTextures().texIds[i].getAsInt();
				CanvasTextureState.ensureTextureOfTextureUnit(TextureData.PROGRAM_SAMPLERS + i, bindTarget, bind);
			}
		}

		// Set this back so nothing inadvertently tries to do stuff with array texture/shadowmap.
		// Was seeing stray invalid operations errors in GL without.
		CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);

		texture.enable(blur);
		transparency.enable();
		depthTest.enable();
		writeMask.enable();
		decal.enable();

		CULL_STATE.setEnabled(cull);
		LIGHTMAP_STATE.setEnabled(true);
		LINE_STATE.setEnabled(lines);

		shader.updateContextInfo(texture.spriteIndex(), target.index);
		shader.setModelOrigin(x, y, z);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("stateIndex:   ").append(index).append("\n");
		sb.append("stateKey      ").append(Strings.padStart(Long.toHexString(bits), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(bits), 64, '0')).append("\n");
		sb.append("primaryTargetTransparency: ").append(primaryTargetTransparency).append("\n");
		sb.append("target: ").append(target.name).append("\n");
		sb.append("texture: ").append(texture.index).append("  ").append(texture.id.toString()).append("\n");
		sb.append("blur: ").append(blur).append("\n");
		sb.append("transparency: ").append(transparency.name).append("\n");
		sb.append("depthTest: ").append(depthTest.name).append("\n");
		sb.append("cull: ").append(cull).append("\n");
		sb.append("writeMask: ").append(writeMask.name).append("\n");
		sb.append("decal: ").append(decal.name).append("\n");
		sb.append("lines: ").append(lines).append("\n");
		return sb.toString();
	}

	// packs render order sorting weights - higher (later) weights are drawn first
	// assumes draws are for a single target and primitive type, so those are not included
	private static final BitPacker64<Void> SORT_PACKER = new BitPacker64<> (null, null);

	// these aren't order-dependent, they are included in sort to minimize state changes
	private static final BitPacker64<Void>.BooleanElement SORT_BLUR = SORT_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.IntElement SORT_DEPTH_TEST = SORT_PACKER.createIntElement(MaterialConstants.DEPTH_TEST_COUNT);
	private static final BitPacker64<Void>.BooleanElement SORT_CULL = SORT_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement SORT_LINES = SORT_PACKER.createBooleanElement();

	// decal should be drawn after non-decal
	private static final BitPacker64<Void>.IntElement SORT_DECAL = SORT_PACKER.createIntElement(MaterialConstants.DECAL_COUNT);
	private static final BitPacker64<Void>.BooleanElement SORT_TPP = SORT_PACKER.createBooleanElement();
	// draw solid first, then various translucent layers
	private static final BitPacker64<Void>.IntElement SORT_TRANSPARENCY = SORT_PACKER.createIntElement(MaterialConstants.TRANSPARENCY_COUNT);
	// draw things that update depth buffer first
	private static final BitPacker64<Void>.IntElement SORT_WRITE_MASK = SORT_PACKER.createIntElement(MaterialConstants.WRITE_MASK_COUNT);

	private static final BinaryRenderState CULL_STATE = new BinaryRenderState(GFX::enableCull, GFX::disableCull);

	@SuppressWarnings("resource")
	private static final BinaryRenderState LIGHTMAP_STATE = new BinaryRenderState(
		//UGLY: vanilla handles binding before uniform upload but we need to do it here
		//so that we don't bind the lightmap texture to some random texture unit
		() -> {
			CanvasTextureState.activeTextureUnit(TextureData.MC_LIGHTMAP);
			Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
		},
		() -> {
			CanvasTextureState.activeTextureUnit(TextureData.MC_LIGHTMAP);
			Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
		});

	private static final BinaryRenderState LINE_STATE = new BinaryRenderState(
		() -> RenderSystem.lineWidth(Math.max(2.5F, Minecraft.getInstance().getWindow().getWidth() / 1920.0F * 2.5F)),
		() -> RenderSystem.lineWidth(1.0F));

	public static void disable() {
		if (active == null && shadowActive == null) {
			return;
		}

		active = null;
		shadowActive = null;
		currentMatrixState = null;
		shadowCurrentMatrixState = null;

		GFX.glDisable(GFX.GL_POLYGON_OFFSET_FILL);
		GFX.glCullFace(GFX.GL_BACK);

		GFX.useProgram(0);
		DecalRenderState.disable();
		TransparencyRenderState.disable();
		DepthTestRenderState.disable();
		WriteMaskRenderState.disable();
		CULL_STATE.disable();
		LIGHTMAP_STATE.disable();
		LINE_STATE.disable();
		TextureMaterialState.disable();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		MaterialIndexTexture.disable();

		if (Pipeline.shadowMapDepth != -1) {
			CanvasTextureState.activeTextureUnit(TextureData.SHADOWMAP);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_2D_ARRAY, 0);
		}

		TargetRenderState.disable();
		CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
	}

	public static final int MAX_COUNT = 4096;
	static int nextIndex = 0;
	static final RenderState[] STATES = new RenderState[MAX_COUNT];
	static final Long2ObjectOpenHashMap<RenderState> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	private static RenderState active = null;
	private static RenderState shadowActive = null;
	private static MatrixState currentMatrixState = null;
	private static MatrixState shadowCurrentMatrixState = null;

	private static RenderState missing;

	private static MaterialTextureManager textures;

	public static void init(MaterialTextureManager texturesIn) {
		textures = texturesIn;
		missing = new RenderState(0);
		STATES[0] = missing;
	}

	public static RenderState missing() {
		assert missing != null;
		return missing;
	}

	public static RenderState fromIndex(int index) {
		return STATES[index];
	}

	public static synchronized RenderState fromBits(long bits) {
		bits &= MaterialStateEncoder.RENDER_STATE_MASK;

		RenderState result = RenderState.MAP.get(bits);

		if (result == null) {
			result = new RenderState(bits);
			RenderState.MAP.put(bits, result);
			RenderState.STATES[result.index] = result;
		}

		return result;
	}

	//	public static boolean enablePrint = false;
}
