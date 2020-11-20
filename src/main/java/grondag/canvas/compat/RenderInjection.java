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
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public interface RenderInjection {
	CallbackInfo CALLBACK_INFO = new CallbackInfo("render", false);

	RenderInjection EMPTY = (worldRenderer, matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f) -> {
	};

	void render(WorldRenderer worldRenderer, MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f);

	static RenderInjection find(String modId, String debugName, String mixinName) {
		if (!FabricLoader.getInstance().isModLoaded(modId)) {
			return EMPTY;
		}

		Method candidate = null;

		for (final Method method : WorldRenderer.class.getDeclaredMethods()) {
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

		return (worldRenderer, matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f) -> {
			if (!caught[0]) {
				try {
					handle.invokeExact(worldRenderer, matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f, CALLBACK_INFO);
				} catch (final Throwable throwable) {
					CanvasMod.LOG.warn("Unable to call " + debugName + " hook due to exception: ", throwable);
					CanvasMod.LOG.warn("Subsequent errors will be suppressed");
					caught[0] = true;
				}
			}
		};
	}
}
