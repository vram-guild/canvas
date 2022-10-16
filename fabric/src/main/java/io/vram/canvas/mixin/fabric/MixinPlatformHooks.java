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

package io.vram.canvas.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import io.vram.frex.api.renderloop.WorldRenderContextBase;

import grondag.canvas.CanvasPlatformHooks;

@Mixin(CanvasPlatformHooks.class)
public class MixinPlatformHooks {
	/**
	 * We want this code to be absent if Fabric isn't loaded so that
	 * we don't have a hard dependency on Fabric API. Application
	 * controlled by mixin plugin.
	 *
	 * @author grondag
	 * @reason Fabric API compatibility
	 */
	@Overwrite(remap = false)
	public static boolean renderCustomSky(WorldRenderContextBase context, Runnable fogSetup) {
		if (context.world() != null) {
			final DimensionRenderingRegistry.SkyRenderer renderer = DimensionRenderingRegistry.getSkyRenderer(context.world().dimension());

			if (renderer != null) {
				fogSetup.run();
				renderer.render((WorldRenderContext) context);
				return true;
			}
		}

		return false;
	}

	/**
	 * We want this code to be absent if Fabric isn't loaded so that
	 * we don't have a hard dependency on Fabric API. Application
	 * controlled by mixin plugin.
	 *
	 * @author grondag
	 * @reason Fabric API compatibility
	 */
	@Overwrite(remap = false)
	public static boolean renderCustomClouds(WorldRenderContextBase context) {
		if (context.world() != null) {
			final DimensionRenderingRegistry.CloudRenderer renderer = DimensionRenderingRegistry.getCloudRenderer(context.world().dimension());

			if (renderer != null) {
				renderer.render((WorldRenderContext) context);
				return true;
			}
		}

		return false;
	}

	/**
	 * We want this code to be absent if Fabric isn't loaded so that
	 * we don't have a hard dependency on Fabric API. Application
	 * controlled by mixin plugin.
	 *
	 * @author grondag
	 * @reason Fabric API compatibility
	 */
	@Overwrite(remap = false)
	public static boolean renderCustomWeather(WorldRenderContextBase context) {
		if (context.world() != null) {
			final DimensionRenderingRegistry.WeatherRenderer renderer = DimensionRenderingRegistry.getWeatherRenderer(context.world().dimension());

			if (renderer != null) {
				renderer.render((WorldRenderContext) context);
				return true;
			}
		}

		return false;
	}
}
