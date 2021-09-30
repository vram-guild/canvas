/*
 * Copyright © Contributing Authors
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

package grondag.canvas.render.frustum;

import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.terrain.region.RenderRegionStorage;

public class RegionCullingFrustum extends FastFrustum {
	private final WorldRenderState worldRenderState;

	public boolean enableRegionCulling = false;

	/** Exclusive world height upper limit. */
	private int worldTopY;

	/** Inclusive world height lower limit. */
	private int worldBottomY;

	public RegionCullingFrustum(WorldRenderState worldRenderState) {
		this.worldRenderState = worldRenderState;
	}

	@Override
	public void prepare(Matrix4f modelMatrix, float tickDelta, Camera camera, Matrix4f projectionMatrix) {
		super.prepare(modelMatrix, tickDelta, camera, projectionMatrix);

		final ClientLevel world = worldRenderState.getWorld();
		worldBottomY = world.getMinBuildHeight();
		worldTopY = world.getMaxBuildHeight();
	}

	@Override
	public boolean cubeInFrustum(double x0, double y0, double z0, double x1, double y1, double z1) {
		if (super.cubeInFrustum(x0, y0, z0, x1, y1, z1)) {
			if (enableRegionCulling) {
				// Always assume entities outside world vertical range are visible
				if (y1 >= worldTopY || y0 < worldBottomY) {
					return true;
				}

				final int rx0 = Mth.floor(x0) & 0xFFFFFFF0;
				final int ry0 = Mth.floor(y0) & 0xFFFFFFF0;
				final int rz0 = Mth.floor(z0) & 0xFFFFFFF0;
				final int rx1 = Mth.floor(x1) & 0xFFFFFFF0;
				final int ry1 = Mth.floor(y1) & 0xFFFFFFF0;
				final int rz1 = Mth.floor(z1) & 0xFFFFFFF0;

				int flags = rx0 == rz1 ? 0 : 1;
				if (ry0 != ry1) flags |= 2;
				if (rz0 != rz1) flags |= 4;

				final RenderRegionStorage regions = worldRenderState.renderRegionStorage;

				switch (flags) {
					case 0b000:
						return regions.isPotentiallyVisible(rx0, ry0, rz0);

					case 0b001:
						return regions.isPotentiallyVisible(rx0, ry0, rz0) || regions.isPotentiallyVisible(rx1, ry0, rz0);

					case 0b010:
						return regions.isPotentiallyVisible(rx0, ry0, rz0) || regions.isPotentiallyVisible(rx0, ry1, rz0);

					case 0b011:
						return regions.isPotentiallyVisible(rx0, ry0, rz0) || regions.isPotentiallyVisible(rx1, ry0, rz0)
								|| regions.isPotentiallyVisible(rx0, ry1, rz0) || regions.isPotentiallyVisible(rx1, ry1, rz0);

					case 0b100:
						return regions.isPotentiallyVisible(rx0, ry0, rz0) || regions.isPotentiallyVisible(rx0, ry0, rz1);

					case 0b101:
						return regions.isPotentiallyVisible(rx0, ry0, rz0) || regions.isPotentiallyVisible(rx1, ry0, rz0)
								|| regions.isPotentiallyVisible(rx0, ry0, rz1) || regions.isPotentiallyVisible(rx1, ry0, rz1);

					case 0b110:
						return regions.isPotentiallyVisible(rx0, ry0, rz0) || regions.isPotentiallyVisible(rx0, ry1, rz0)
								|| regions.isPotentiallyVisible(rx0, ry0, rz1) || regions.isPotentiallyVisible(rx0, ry1, rz1);

					case 0b111:
						return regions.isPotentiallyVisible(rx0, ry0, rz0) || regions.isPotentiallyVisible(rx1, ry0, rz0)
								|| regions.isPotentiallyVisible(rx0, ry1, rz0) || regions.isPotentiallyVisible(rx1, ry1, rz0)
								|| regions.isPotentiallyVisible(rx0, ry0, rz1) || regions.isPotentiallyVisible(rx1, ry0, rz1)
								|| regions.isPotentiallyVisible(rx0, ry1, rz1) || regions.isPotentiallyVisible(rx1, ry1, rz1);
					default:
						assert false : "Pathological spatial test result in RegionCullingFrustum";
						return true;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}
}
