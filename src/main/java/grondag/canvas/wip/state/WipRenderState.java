/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.wip.state;

import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.Configurator;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.pipeline.CanvasFrameBufferHacks;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.canvas.wip.shader.WipGlProgram;
import grondag.canvas.wip.state.property.WipDecal;
import grondag.canvas.wip.state.property.WipTarget;
import grondag.canvas.wip.state.property.WipTextureState;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;

/**
 * Primitives with the same state have the same vertex encoding,
 * same uniform state and same GL draw state. Analogous to RenderLayer<p>
 *
 * Also serves as the key for vertex collection. Primitives with the same state
 * can share the same draw call and should be packed contiguously in the buffer.<p>
 *
 * Primitives must have the same sorting requirements, which for all but the translucent
 * collection keys means there is no sorting. Translucent primitives that require sorting
 * all belong to a small handful of collectors.<p>
 *
 * Vertex data with different state can share the same buffer and should be
 * packed in glState, uniformState order for best performance.
 */
public final class WipRenderState extends AbstractRenderState {
	protected WipRenderState(long bits) {
		super(nextIndex++, bits);
	}

	@SuppressWarnings("resource")
	public void enable() {
		target.enable();

		if (texture == WipTextureState.NO_TEXTURE) {
			RenderSystem.disableTexture();
		} else {
			RenderSystem.enableTexture();
			final AbstractTexture tex = texture.texture();
			tex.bindTexture();
			tex.setFilter(bilinear, true);

			if (texture.isAtlas()) {
				texture.atlasInfo().enable();
			}
		}

		translucency.action.run();
		depthTest.action.run();
		writeMask.action.run();
		fog.action.run();
		decal.enable();

		// NB: must be after frame-buffer target switch
		if (Configurator.enableBloom) {
			CanvasFrameBufferHacks.startEmissiveCapture();
		} else {
			CanvasFrameBufferHacks.endEmissiveCapture();
		}

		RenderSystem.shadeModel(GL11.GL_SMOOTH);

		if (cull) {
			RenderSystem.enableCull();
		} else {
			RenderSystem.disableCull();
		}

		if (enableLightmap) {
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
		} else {
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
		}

		if (lines) {
			RenderSystem.lineWidth(Math.max(2.5F, MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 1920.0F * 2.5F));
		} else {
			RenderSystem.lineWidth(1.0F);
		}

		shader.activate(texture.atlasInfo());
	}

	public static void disable() {
		// NB: must be before frame-buffer target switch
		if (Configurator.enableBloom) CanvasFrameBufferHacks.endEmissiveCapture();

		MaterialVertexFormat.disableDirect();
		WipGlProgram.deactivate();
		RenderSystem.shadeModel(GL11.GL_FLAT);
		SpriteInfoTexture.disable();
		WipDecal.disable();
		WipTarget.disable();
	}

	public static final int MAX_COUNT = 4096;
	static int nextIndex = 0;
	static final WipRenderState[] STATES = new WipRenderState[MAX_COUNT];
	private static final Long2ObjectOpenHashMap<WipRenderState> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	public static final WipRenderState MISSING = new WipRenderState(0);

	static {
		STATES[0] = MISSING;
	}

	public static WipRenderState fromIndex(int index) {
		return STATES[index];
	}

	private static ThreadLocal<Finder> FINDER = ThreadLocal.withInitial(Finder::new);

	public static Finder finder() {
		final Finder result = FINDER.get();
		result.reset();
		return result;
	}

	public static class Finder extends AbstractStateFinder<Finder, WipRenderState>{
		@Override
		public synchronized WipRenderState find() {
			WipRenderState result = MAP.get(bits);

			if (result == null) {
				result = new WipRenderState(bits);
				MAP.put(bits, result);
				STATES[result.index] = result;
			}

			return result;
		}

		@Override
		protected WipRenderState missing() {
			return MISSING;
		}
	}
}
