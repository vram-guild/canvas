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

package grondag.canvas.compat;

import grondag.canvas.CanvasMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class CampanionHolder {

	private static final CampanionRender DUMMY = (worldRenderer, matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f) -> {
	};
	public static CampanionRender handler = DUMMY;

	static {
		if (FabricLoader.getInstance().isModLoaded("campanion")) {
			MethodHandles.Lookup lookup = MethodHandles.lookup();

			for (Method method : WorldRenderer.class.getDeclaredMethods()) {
				MixinMerged annotation = method.getAnnotation(MixinMerged.class);

				if (annotation != null && annotation.mixin().equals("com.terraformersmc.campanion.mixin.client.MixinWorldRenderer")) {
					MethodHandle handle;

					try {
						method.setAccessible(true);
						handle = lookup.unreflect(method);
					} catch (SecurityException | IllegalAccessException e) {
						CanvasMod.LOG.warn("Could not access Campanion's render hooks, compatibility may not be present");
						continue;
					}

					handler = (worldRenderer, matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f) -> {
						try {
							handle.invokeExact(worldRenderer, matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f, new CallbackInfo("render", false));
						} catch (final Throwable e) {
							CanvasMod.LOG.warn("Unable to call Campanion render hook due to exception: ", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							handler = DUMMY;
						}
					};
				}
			}
		}
	}

	public interface CampanionRender {
		void render(WorldRenderer worldRenderer, MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f);
	}
}
