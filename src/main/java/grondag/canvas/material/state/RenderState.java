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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.material.property.BinaryMaterialState;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.render.SkyShadowRenderer;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.MaterialShaderImpl;
import grondag.canvas.shader.ProgramType;
import grondag.canvas.texture.MaterialInfoTexture;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.MatrixState;

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
	protected RenderState(long bits) {
		super(nextIndex++, bits);
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
		if (shadowActive == this) {
			depthShader.setModelOrigin(x, y, z);
			depthShader.setCascade(cascade);
			return;
		}

		if (shadowActive == null) {
			// same for all, so only do 1X
			RenderSystem.shadeModel(GL11.GL_SMOOTH);
			Pipeline.skyShadowFbo.bind();
		}

		shadowActive = this;
		active = null;

		if (programType == ProgramType.MATERIAL_VERTEX_LOGIC) {
			MaterialInfoTexture.INSTANCE.enable();
		} else {
			MaterialInfoTexture.INSTANCE.disable();
		}

		// WIP: can probably remove many of these

		// controlled in shader
		RenderSystem.disableAlphaTest();

		texture.enable(blur);
		transparency.enable();
		depthTest.enable();
		writeMask.enable();
		fog.enable();
		// WIP: disable decal renders in depth pass
		decal.enable();

		CULL_STATE.setEnabled(cull);
		LIGHTMAP_STATE.setEnabled(true);
		LINE_STATE.setEnabled(lines);

		depthShader.activate(this);
		depthShader.setContextInfo(texture.atlasInfo(), target.index);
		depthShader.setModelOrigin(x, y, z);
		depthShader.setCascade(cascade);

		GL46.glEnable(GL46.GL_POLYGON_OFFSET_FILL);
		GL46.glPolygonOffset(Pipeline.shadowSlopeFactor, Pipeline.shadowBiasUnits);
		//GL46.glCullFace(GL46.GL_FRONT);
	}

	private void enableMaterial(int x, int y, int z) {
		assert CanvasGlHelper.checkError();

		final MaterialShaderImpl shader = MatrixState.get() == MatrixState.SCREEN ? guiShader : this.shader;

		if (active == this) {
			shader.setModelOrigin(x, y, z);
			return;
		}

		//		if (enablePrint) {
		//			GlStateSpy.print();
		//		}

		if (active == null) {
			// same for all, so only do 1X
			RenderSystem.shadeModel(GL11.GL_SMOOTH);
			target.enable();
		} else if (active.target != target) {
			target.enable();
		}

		assert CanvasGlHelper.checkError();

		active = this;
		shadowActive = null;

		if (programType.isVertexLogic) {
			MaterialInfoTexture.INSTANCE.enable();
		} else {
			MaterialInfoTexture.INSTANCE.disable();
		}

		assert CanvasGlHelper.checkError();

		// controlled in shader
		RenderSystem.disableAlphaTest();
		assert CanvasGlHelper.checkError();

		if (Pipeline.shadowMapDepth != -1) {
			CanvasTextureState.activeTextureUnit(TextureData.SHADOWMAP);
			CanvasTextureState.bindTexture(GL46.GL_TEXTURE_2D_ARRAY, Pipeline.shadowMapDepth);
			assert CanvasGlHelper.checkError();
			// Set this back so nothing inadvertently tries to do stuff with array texture/shadowmap.
			// Was seeing stray invalid operations errors in GL without.
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}

		texture.enable(blur);
		assert CanvasGlHelper.checkError();

		transparency.enable();
		assert CanvasGlHelper.checkError();

		depthTest.enable();
		assert CanvasGlHelper.checkError();

		writeMask.enable();
		fog.enable();
		decal.enable();

		assert CanvasGlHelper.checkError();

		CULL_STATE.setEnabled(cull);
		LIGHTMAP_STATE.setEnabled(true);
		LINE_STATE.setEnabled(lines);

		assert CanvasGlHelper.checkError();

		shader.activate(this);
		shader.setContextInfo(texture.atlasInfo(), target.index);
		shader.setModelOrigin(x, y, z);

		assert CanvasGlHelper.checkError();
	}

	private static final BinaryMaterialState CULL_STATE = new BinaryMaterialState(RenderSystem::enableCull, RenderSystem::disableCull);

	private static final BinaryMaterialState LIGHTMAP_STATE = new BinaryMaterialState(
		() -> MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable(),
		() -> MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable());

	private static final BinaryMaterialState LINE_STATE = new BinaryMaterialState(
		() -> RenderSystem.lineWidth(Math.max(2.5F, MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 1920.0F * 2.5F)),
		() -> RenderSystem.lineWidth(1.0F));

	public static void disable() {
		if (active == null && shadowActive == null) {
			return;
		}

		active = null;
		shadowActive = null;

		GL46.glDisable(GL46.GL_POLYGON_OFFSET_FILL);
		GL46.glCullFace(GL46.GL_BACK);

		CanvasVertexFormat.disableDirect();
		GlProgram.deactivate();
		RenderSystem.shadeModel(GL11.GL_FLAT);
		SpriteInfoTexture.disable();
		MaterialDecal.disable();
		MaterialTransparency.disable();
		MaterialDepthTest.disable();
		MaterialWriteMask.disable();
		MaterialFog.disable();
		CULL_STATE.disable();
		LIGHTMAP_STATE.disable();
		LINE_STATE.disable();
		MaterialTextureState.disable();
		RenderSystem.color4f(1f, 1f, 1f, 1f);
		MaterialInfoTexture.INSTANCE.disable();

		if (Pipeline.shadowMapDepth != -1) {
			CanvasTextureState.activeTextureUnit(TextureData.SHADOWMAP);
			CanvasTextureState.bindTexture(GL46.GL_TEXTURE_2D_ARRAY, 0);
		}

		MaterialTarget.disable();
		CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);

		assert CanvasGlHelper.checkError();
		//		if (enablePrint) {
		//			GlStateSpy.print();
		//			enablePrint = false;
		//		}
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
