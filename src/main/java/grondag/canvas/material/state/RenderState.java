/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.material.state;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.config.Configurator;
import grondag.canvas.material.property.BinaryMaterialState;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.render.world.SkyShadowRenderer;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.MaterialShaderImpl;
import grondag.canvas.shader.data.MatrixState;
import grondag.canvas.texture.MaterialIndexTexture;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;
import grondag.canvas.vf.VfInt;
import grondag.canvas.vf.VfVertex;
import grondag.fermion.bits.BitPacker64;

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
public final class RenderState extends AbstractRenderState {
	// packs render order sorting weights - higher (later) weights are drawn first
	// assumes draws are for a single target and primitive type, so those are not included
	private static final BitPacker64<Void> SORT_PACKER = new BitPacker64<> (null, null);

	// these aren't order-dependent, they are included in sort to minimize state changes
	private static final BitPacker64<Void>.BooleanElement SORT_BLUR = SORT_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.IntElement SORT_DEPTH_TEST = SORT_PACKER.createIntElement(MaterialDepthTest.DEPTH_TEST_COUNT);
	private static final BitPacker64<Void>.BooleanElement SORT_CULL = SORT_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement SORT_LINES = SORT_PACKER.createBooleanElement();

	// decal should be drawn after non-decal
	private static final BitPacker64<Void>.IntElement SORT_DECAL = SORT_PACKER.createIntElement(MaterialDecal.DECAL_COUNT);
	// primary sorted layer drawn first
	private static final BitPacker64<Void>.BooleanElement SORT_TPP = SORT_PACKER.createBooleanElement();
	// draw solid first, then various translucent layers
	private static final BitPacker64<Void>.IntElement SORT_TRANSPARENCY = SORT_PACKER.createIntElement(MaterialTransparency.TRANSPARENCY_COUNT);
	// draw things that update depth buffer first
	private static final BitPacker64<Void>.IntElement SORT_WRITE_MASK = SORT_PACKER.createIntElement(MaterialWriteMask.WRITE_MASK_COUNT);

	public final long drawPriority;

	protected RenderState(long bits) {
		super(nextIndex++, bits);
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
		final boolean vf = Configurator.vf && MatrixState.get() == MatrixState.REGION;
		final MaterialShaderImpl depthShader = vf ? vfDepthShader : this.depthShader;

		if (shadowActive == this) {
			depthShader.setModelOrigin(x, y, z);
			depthShader.setCascade(cascade);
			return;
		}

		if (shadowActive == null) {
			// same for all, so only do 1X
			Pipeline.skyShadowFbo.bind();
		}

		shadowActive = this;
		active = null;
		texture.materialIndexProvider().enable();

		if (vf) {
			VfInt.COLOR.enable();
			VfInt.UV.enable();
			VfVertex.VERTEX.enable();
		}

		texture.enable(blur);
		transparency.enable();
		depthTest.enable();
		writeMask.enable();

		// NB: Could probably disable decal renders in depth pass for most use cases
		// but there's nothing to prevent anyone from rendering stacked cutout decals.
		// Decals aren't super common so left in for now.
		decal.enable();

		CULL_STATE.setEnabled(cull);
		LIGHTMAP_STATE.setEnabled(true);
		LINE_STATE.setEnabled(lines);

		depthShader.updateContextInfo(texture.atlasInfo(), target.index);
		depthShader.setModelOrigin(x, y, z);
		depthShader.setCascade(cascade);

		GFX.enable(GFX.GL_POLYGON_OFFSET_FILL);
		GFX.polygonOffset(Pipeline.shadowSlopeFactor, Pipeline.shadowBiasUnits);
		//GL46.glCullFace(GL46.GL_FRONT);
	}

	private void enableMaterial(int x, int y, int z) {
		final MaterialShaderImpl shader;

		switch (MatrixState.get()) {
			case REGION:
				shader = Configurator.vf ? vfShader : this.shader;
				break;
			case SCREEN:
				shader = guiShader;
				break;
			case CAMERA:
			default:
				shader = this.shader;
				break;
		}

		if (active == this) {
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
		shadowActive = null;
		texture.materialIndexProvider().enable();

		if (shader.programType.vf) {
			VfInt.COLOR.enable();
			VfInt.UV.enable();
			VfVertex.VERTEX.enable();
			assert MatrixState.get() == MatrixState.REGION;
		}

		if (Pipeline.shadowMapDepth != -1) {
			CanvasTextureState.activeTextureUnit(TextureData.SHADOWMAP);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_2D_ARRAY, Pipeline.shadowMapDepth);

			CanvasTextureState.activeTextureUnit(TextureData.SHADOWMAP_TEXTURE);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_2D_ARRAY, Pipeline.shadowMapDepth);
			// Set this back so nothing inadvertently tries to do stuff with array texture/shadowmap.
			// Was seeing stray invalid operations errors in GL without.
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}

		if (Pipeline.config().materialProgram.samplerNames.length > 0) {
			// Activate non-frex material program textures
			for (int i = 0; i < Pipeline.config().materialProgram.samplerNames.length; i++) {
				final int bindTarget = Pipeline.materialTextures().texTargets[i];
				final int bind = Pipeline.materialTextures().texIds[i];
				CanvasTextureState.activeTextureUnit(TextureData.PROGRAM_SAMPLERS + i);
				CanvasTextureState.bindTexture(bindTarget, bind);
			}

			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}

		texture.enable(blur);
		transparency.enable();
		depthTest.enable();
		writeMask.enable();
		decal.enable();

		CULL_STATE.setEnabled(cull);
		LIGHTMAP_STATE.setEnabled(true);
		LINE_STATE.setEnabled(lines);

		shader.updateContextInfo(texture.atlasInfo(), target.index);
		shader.setModelOrigin(x, y, z);
	}

	private static final BinaryMaterialState CULL_STATE = new BinaryMaterialState(GFX::enableCull, GFX::disableCull);

	@SuppressWarnings("resource")
	private static final BinaryMaterialState LIGHTMAP_STATE = new BinaryMaterialState(
		//UGLY: vanilla handles binding before uniform upload but we need to do it here
		//so that we don't bind the lightmap texture to some random texture unit
		() -> {
			CanvasTextureState.activeTextureUnit(TextureData.MC_LIGHTMAP);
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
		},
		() -> {
			CanvasTextureState.activeTextureUnit(TextureData.MC_LIGHTMAP);
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
		});

	private static final BinaryMaterialState LINE_STATE = new BinaryMaterialState(
		() -> RenderSystem.lineWidth(Math.max(2.5F, MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 1920.0F * 2.5F)),
		() -> RenderSystem.lineWidth(1.0F));

	public static void disable() {
		if (active == null && shadowActive == null) {
			return;
		}

		active = null;
		shadowActive = null;

		GFX.glDisable(GFX.GL_POLYGON_OFFSET_FILL);
		GFX.glCullFace(GFX.GL_BACK);

		GlProgram.deactivate();
		MaterialDecal.disable();
		MaterialTransparency.disable();
		MaterialDepthTest.disable();
		MaterialWriteMask.disable();
		CULL_STATE.disable();
		LIGHTMAP_STATE.disable();
		LINE_STATE.disable();
		MaterialTextureState.disable();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		MaterialIndexTexture.disable();

		if (Configurator.vf) {
			VfInt.COLOR.disable();
			VfInt.UV.disable();
			VfVertex.VERTEX.disable();
		}

		if (Pipeline.shadowMapDepth != -1) {
			CanvasTextureState.activeTextureUnit(TextureData.SHADOWMAP);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_2D_ARRAY, 0);
		}

		MaterialTarget.disable();
		CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
	}

	public static final int MAX_COUNT = 4096;
	static int nextIndex = 0;
	static final RenderState[] STATES = new RenderState[MAX_COUNT];
	static final Long2ObjectOpenHashMap<RenderState> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	private static RenderState active = null;
	private static RenderState shadowActive = null;

	public static final RenderState MISSING = new RenderState(0);

	static {
		STATES[0] = MISSING;
	}

	public static RenderState fromIndex(int index) {
		return STATES[index];
	}

	//	public static boolean enablePrint = false;
}
