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
import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import net.minecraft.client.renderer.LevelRenderer;

import net.fabricmc.loader.api.FabricLoader;

import io.vram.frex.api.renderloop.WorldRenderContext;

import grondag.canvas.CanvasMod;

interface RenderInjection {
	CallbackInfo CALLBACK_INFO = new CallbackInfo("render", false);

	RenderInjection EMPTY = ctx -> { };

	void render(WorldRenderContext ctx);

	static RenderInjection find(String modId, String debugName, String mixinName) {
		if (!FabricLoader.getInstance().isModLoaded(modId)) {
			return EMPTY;
		}

		Method candidate = null;

		for (final Method method : LevelRenderer.class.getDeclaredMethods()) {
			final MixinMerged annotation = method.getAnnotation(MixinMerged.class);

			if (annotation != null && mixinName.equals(annotation.mixin())) {
				candidate = method;
			}
		}

		if (candidate == null) {
			CanvasMod.LOG.warn("No candidate was found for " + modId + " by the mixin " + mixinName + ", compatibility may be limited");
			return EMPTY;
		}

		return of(debugName, candidate);
	}

	static RenderInjection of(String debugName, Method method) {
		try {
			method.setAccessible(true);
			return of(debugName, MethodHandles.lookup().unreflect(method));
		} catch (final Throwable throwable) {
			CanvasMod.LOG.warn("Unable to initialize compatibility for " + debugName + " due to exception: ", throwable);
			return EMPTY;
		}
	}

	static RenderInjection of(String debugName, MethodHandle handle) {
		final boolean[] caught = new boolean[1];

		return ctx -> {
			if (!caught[0]) {
				try {
					handle.invokeExact(ctx.worldRenderer(), ctx.poseStack(), ctx.tickDelta(), ctx.limitTime(), ctx.blockOutlines(), ctx.camera(), ctx.gameRenderer(), ctx.lightmapTexture(), ctx.projectionMatrix(), CALLBACK_INFO);
				} catch (final Throwable throwable) {
					CanvasMod.LOG.warn("Unable to call " + debugName + " hook due to exception: ", throwable);
					CanvasMod.LOG.warn("Subsequent errors will be suppressed");
					caught[0] = true;
				}
			}
		};
	}
}
