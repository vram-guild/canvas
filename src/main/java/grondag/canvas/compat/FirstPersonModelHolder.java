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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class FirstPersonModelHolder {
	private static final CameraHandler DEFAULT_CAMERA = () -> false;
	private static final RenderHandler DEFAULT_RENDER = (b) -> { };

	public static CameraHandler cameraHandler = DEFAULT_CAMERA;
	public static RenderHandler renderHandler = DEFAULT_RENDER;

	private static MethodHandle boundApplyThirdPersonHandle;

	static {
		if (FabricLoader.getInstance().isModLoaded("firstperson")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> fpmCore = Class.forName("dev.tr7zw.firstperson.FirstPersonModelCore");
				final Class<?> clazz = Class.forName("dev.tr7zw.firstperson.fabric.FabricWrapper");
				final Method applyThirdPerson = clazz.getDeclaredMethod("applyThirdPerson", boolean.class);
				final MethodHandle applyThirdPersonHandle = lookup.unreflect(applyThirdPerson);
				final Field isRenderingPlayer = fpmCore.getField("isRenderingPlayer");

				cameraHandler = () -> {
					try {
						if (boundApplyThirdPersonHandle == null) {
							final Object wrapperObject = fpmCore.getField("wrapper").get(null);

							boundApplyThirdPersonHandle = applyThirdPersonHandle.bindTo(wrapperObject);
						}

						return (boolean) boundApplyThirdPersonHandle.invokeExact(false);
					} catch (final Throwable e) {
						CanvasMod.LOG.warn("Unable to deffer to FirstPersonModel due to exception: ", e);
						CanvasMod.LOG.warn("Subsequent errors will be suppressed");
						return (cameraHandler = DEFAULT_CAMERA).renderFirstPersonPlayer();
					}
				};

				renderHandler = (b) -> {
					try {
						isRenderingPlayer.setBoolean(null, b);
					} catch (final Throwable e) {
						CanvasMod.LOG.warn("Unable to deffer to FirstPersonModel due to exception: ", e);
						CanvasMod.LOG.warn("Subsequent errors will be suppressed");

						renderHandler = DEFAULT_RENDER;
					}
				};
			} catch (final Throwable throwable) {
				CanvasMod.LOG.warn("Unable to initialize compatibility for FirstPersonModel due to exception: ", throwable);
			}
		}
	}

	public interface CameraHandler {
		boolean renderFirstPersonPlayer();
	}

	public interface RenderHandler {
		void setIsRenderingPlayer(boolean b);
	}
}
