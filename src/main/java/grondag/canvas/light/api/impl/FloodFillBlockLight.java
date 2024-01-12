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

package grondag.canvas.light.api.impl;

import grondag.canvas.light.api.BlockLight;
import grondag.canvas.light.color.LightOp;

public final class FloodFillBlockLight implements BlockLight {
	public final short value;
	public final boolean levelIsSet;
	public final float red, green, blue, lightLevel;

	public FloodFillBlockLight(short value, boolean levelIsSet) {
		this.value = value;
		this.levelIsSet = levelIsSet;
		this.lightLevel = Math.max(LightOp.R.of(value), Math.max(LightOp.G.of(value), LightOp.B.of(value)));
		this.red = LightOp.R.of(value) / 15.0f;
		this.green = LightOp.G.of(value) / 15.0f;
		this.blue = LightOp.B.of(value) / 15.0f;
	}

	public FloodFillBlockLight(float lightLevel, float red, float green, float blue, boolean levelIsSet) {
		this(computeValue(lightLevel, red, green, blue), levelIsSet);
	}

	public FloodFillBlockLight withLevel(float lightEmission) {
		if (this.lightLevel() == lightEmission && this.levelIsSet) {
			return this;
		} else {
			return new FloodFillBlockLight(lightEmission, red(), green(), blue(), true);
		}
	}

	static short computeValue(float lightLevel, float red, float green, float blue) {
		final int blockRadius = lightLevel == 0f ? 0 : org.joml.Math.clamp(1, 15, Math.round(lightLevel));
		return LightOp.encode(clampLight(blockRadius * red), clampLight(blockRadius * green), clampLight(blockRadius * blue), 0);
	}

	private static int clampLight(float light) {
		return org.joml.Math.clamp(0, 15, Math.round(light));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof BlockLight that)) {
			return false;
		}

		if (this.lightLevel() == 0f && that.lightLevel() == 0f) {
			// both are completely dark regardless of color
			return true;
		}

		if (obj instanceof FloodFillBlockLight floodFillBlockLight) {
			return this.value == floodFillBlockLight.value && this.levelIsSet == floodFillBlockLight.levelIsSet;
		}

		if (that.red() != this.red()) {
			return false;
		}

		if (that.green() != this.green()) {
			return false;
		}

		if (that.blue() != this.blue()) {
			return false;
		}

		return that.lightLevel() == this.lightLevel();
	}

	@Override
	public float lightLevel() {
		return lightLevel;
	}

	@Override
	public float red() {
		return red;
	}

	@Override
	public float green() {
		return green;
	}

	@Override
	public float blue() {
		return blue;
	}
}
