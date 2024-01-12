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

package grondag.canvas.light.api;

import grondag.canvas.light.api.impl.FloodFillBlockLight;

/**
 * BlockLight API draft.
 */
public interface BlockLight {
	/**
	 * The light level. Typically, this represents the light radius after multiplied with the
	 * highest color component, but also affects maximum brightness.
	 *
	 * <p>Implementation may choose whether to prioritize the radius aspect or brightness aspect.
	 *
	 * <p>Typical value is in range 0-15. Value outside of this range is implementation-specific.
	 *
	 * <p>In JSON format, defaults to the vanilla registered light level when missing.
	 * Importantly, light level is attached to block states. Fluid states will attempt
	 * to default to their block state counterpart.
	 */
	float lightLevel();

	/**
	 * Red intensity. Behavior of values outside of range 0-1 is undefined.
	 * In JSON format, defaults to 0 when missing.
	 */
	float red();

	/**
	 * Green intensity. Behavior of values outside of range 0-1 is undefined.
	 * In JSON format, defaults to 0 when missing.
	 */
	float green();

	/**
	 * Blue intensity. Behavior of values outside of range 0-1 is undefined.
	 * In JSON format, defaults to 0 when missing.
	 */
	float blue();

	/**
	 * Constructs an implementation consistent instance of BlockLight.
	 */
	static BlockLight of(float lightLevel, float red, float green, float blue) {
		return new FloodFillBlockLight(lightLevel, red, green, blue, true);
	}
}
