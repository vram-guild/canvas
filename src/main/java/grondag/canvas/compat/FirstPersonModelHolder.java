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
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class FirstPersonModelHolder {

	private static final Redirect DEFAULT = (worldRenderer, camera, matrices) -> camera.isThirdPerson();
	public static Redirect handler = DEFAULT;

	static {
		if (FabricLoader.getInstance().isModLoaded("firstperson")) {
			try {
				for (Method method : WorldRenderer.class.getDeclaredMethods()) {
					MixinMerged annotation = method.getAnnotation(MixinMerged.class);

					if (annotation != null
							&& "de.tr7zw.firstperson.mixins.WorldRendererMixin".equals(annotation.mixin())
							&& method.getName().startsWith("redirect$")) {
						method.setAccessible(true);
						MethodHandle handle = MethodHandles.lookup().unreflect(method);
						handler = (worldRenderer, camera, matrices) -> {
							try {
								return (boolean) handle.invokeExact(worldRenderer, camera, matrices);
							} catch (Throwable throwable) {
								CanvasMod.LOG.warn("Unable to deffer to FirstPersonModel due to exception: ", throwable);
								CanvasMod.LOG.warn("Subsequent errors will be suppressed");
								return (handler = DEFAULT).isThirdPerson(worldRenderer, camera, matrices);
							}
						};
					}
				}
			} catch (Throwable throwable) {
				CanvasMod.LOG.warn("Unable to initialize compatibility for FirstPersonModel due to exception: ", throwable);
			}
		}
	}

	public interface Redirect {
		boolean isThirdPerson(WorldRenderer worldRenderer, Camera camera, MatrixStack matrices);
	}
}
