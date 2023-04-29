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

import net.minecraft.world.entity.Entity;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class PlayerAnimatorHolder {
	public static setFPPHandler handlerA;
	public static fakeTPHanlder handlerB;

	private static void reset() {
		handlerA = (b) -> { };
		handlerB = (e) -> false;
	}

	static {
		reset();

		if (FabricLoader.getInstance().isModLoaded("player-animator")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> firstPersonMode = Class.forName("dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode");
				final Method setFirstPersonPass = firstPersonMode.getDeclaredMethod("setFirstPersonPass", boolean.class);
				final MethodHandle setFirstPersonPassHandle = lookup.unreflect(setFirstPersonPass);
				final Object fakeTpType = firstPersonMode.getEnumConstants()[2];
				System.out.println(fakeTpType);

				final Class<?> animatedPlayer = Class.forName("dev.kosmx.playerAnim.impl.IAnimatedPlayer");
				final Method getAnimation = animatedPlayer.getDeclaredMethod("playerAnimator_getAnimation");
				final MethodHandle getAnimationHandle = lookup.unreflect(getAnimation);

				final Class<?> animationProcessor = Class.forName("dev.kosmx.playerAnim.core.impl.AnimationProcessor");
				final Method getFirstPersonMode = animationProcessor.getDeclaredMethod("getFirstPersonMode");
				final MethodHandle getFirstPersonModeHandle = lookup.unreflect(getFirstPersonMode);

				handlerA = (b) -> {
					try {
						setFirstPersonPassHandle.invoke(b);
					} catch (final Throwable e) {
						CanvasMod.LOG.warn("Unable to deffer to FirstPersonModel due to exception: ", e);
						CanvasMod.LOG.warn("Subsequent errors will be suppressed");
						reset();
					}
				};

				handlerB = (ee) -> {
					try {
						var x = getAnimationHandle.invoke(ee);
						return getFirstPersonModeHandle.invoke(x) == fakeTpType;
					} catch (final Throwable e) {
						CanvasMod.LOG.warn("Unable to deffer to FirstPersonModel due to exception: ", e);
						CanvasMod.LOG.warn("Subsequent errors will be suppressed");
						reset();
						return false;
					}
				};
			} catch (final Throwable throwable) {
				CanvasMod.LOG.warn("Unable to initialize compatibility for Player Animator due to exception: ", throwable);
			}
		}
	}

	public interface setFPPHandler {
		void setFirstPerson(boolean b);
	}

	public interface fakeTPHanlder {
		boolean isFakeThirdPerson(Entity e);
	}
}
