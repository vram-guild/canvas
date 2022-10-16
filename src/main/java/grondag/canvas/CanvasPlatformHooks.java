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

package grondag.canvas;

import io.vram.frex.api.renderloop.WorldRenderContextBase;

public class CanvasPlatformHooks {
	/**
	 * Handles Fabric sky renderer hook if Fabric is loaded.
	 * Returns true if hook did something and vanilla render should not be called.
	 *
	 * <p>Note that Fabric will hook into the vanilla method and
	 * expect to see Fabric world context populated. By calling these
	 * before invoking the vanilla method we can avoid dealing with the Fabric context
	 * because the Fabric hook will not trigger if this returns false.
	 */
	public static boolean renderCustomSky(WorldRenderContextBase context, Runnable fogSetup) {
		return false;
	}

	/**
	 * Handles Fabric cloud renderer hook if Fabric is loaded.
	 * Returns true if hook did something and vanilla render should not be called.
	 *
	 * <p>Note that Fabric will hook into the vanilla method and
	 * expect to see Fabric world context populated. By calling these
	 * before invoking the vanilla method we can avoid dealing with the Fabric context
	 * because the Fabric hook will not trigger if this returns false.
	 */
	public static boolean renderCustomClouds(WorldRenderContextBase context) {
		return false;
	}

	/**
	 * Handles Fabric weather renderer hook if Fabric is loaded.
	 * Returns true if hook did something and vanilla render should not be called.
	 *
	 * <p>Note that Fabric will hook into the vanilla method and
	 * expect to see Fabric world context populated. By calling these
	 * before invoking the vanilla method we can avoid dealing with the Fabric context
	 * because the Fabric hook will not trigger if this returns false.
	 */
	public static boolean renderCustomWeather(WorldRenderContextBase context) {
		return false;
	}
}
