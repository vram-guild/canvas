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

package grondag.canvas.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import grondag.canvas.CanvasMod;

class VoxelMapHolder {
	private static final PostRenderHandler DUMMY_RENDER_HANDLER = (LevelRenderer wr, PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f matrix4f) -> { };
	static PostRenderHandler postRenderHandler = DUMMY_RENDER_HANDLER;

	private static final PostRenderLayerHandler DUMMY_RENDER_LAYER_HANDLER = (LevelRenderer wr, RenderType renderLayer, PoseStack matrixStack, double d, double e, double f) -> { };
	static PostRenderLayerHandler postRenderLayerHandler = DUMMY_RENDER_LAYER_HANDLER;

	static {
		if (FabricLoader.getInstance().isModLoaded("voxelmap")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = LevelRenderer.class;

				for (final Method m : clazz.getDeclaredMethods()) {
					final String name = m.getName();

					if (name.contains("handler$")) {
						if (name.endsWith("$postRender")) {
							m.setAccessible(true);
							final MethodHandle handler = lookup.unreflect(m);

							postRenderHandler = (LevelRenderer wr, PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f matrix4f) -> {
								try {
									handler.invokeExact(wr, matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f, (CallbackInfo) null);
								} catch (final Throwable e) {
									CanvasMod.LOG.warn("Unable to call VoxelMap postRender hook due to exception:", e);
									CanvasMod.LOG.warn("Subsequent errors will be suppressed");
									postRenderHandler = DUMMY_RENDER_HANDLER;
								}
							};
						} else if (name.endsWith("$postRenderLayer")) {
							m.setAccessible(true);
							final MethodHandle handler = lookup.unreflect(m);

							postRenderLayerHandler = (LevelRenderer wr, RenderType renderLayer, PoseStack matrixStack, double x, double y, double z) -> {
								try {
									handler.invokeExact(wr, renderLayer, matrixStack, x, y, z, (CallbackInfo) null);
								} catch (final Throwable e) {
									CanvasMod.LOG.warn("Unable to call VoxelMap postRenderLayer hook due to exception:", e);
									CanvasMod.LOG.warn("Subsequent errors will be suppressed");
									postRenderLayerHandler = DUMMY_RENDER_LAYER_HANDLER;
								}
							};
						}
					}
				}

				if (postRenderHandler == DUMMY_RENDER_HANDLER || postRenderLayerHandler == DUMMY_RENDER_LAYER_HANDLER) {
					postRenderLayerHandler = DUMMY_RENDER_LAYER_HANDLER;
					postRenderHandler = DUMMY_RENDER_HANDLER;
					CanvasMod.LOG.warn("Unable to enable all VoxelMap compatibility hooks - method matches not found");
				} else {
					CanvasMod.LOG.info("Found VoxelMap - compatibility hooks enabled");
				}
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to enable all VoxelMap compatibility hooks due to exception:", e);
			}
		}
	}

	interface PostRenderHandler {
		void render(LevelRenderer wr, PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f matrix4f);
	}

	interface PostRenderLayerHandler {
		void render(LevelRenderer wr, RenderType renderLayer, PoseStack matrixStack, double d, double e, double f);
	}
}
