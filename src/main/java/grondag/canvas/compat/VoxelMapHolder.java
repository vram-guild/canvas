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

package grondag.canvas.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;

import net.fabricmc.loader.api.FabricLoader;

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
