/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
