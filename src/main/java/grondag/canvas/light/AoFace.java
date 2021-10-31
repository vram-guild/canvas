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

package grondag.canvas.light;

import static grondag.canvas.light.AoVertexClampFunction.clamp;
import static net.minecraft.core.Direction.DOWN;
import static net.minecraft.core.Direction.EAST;
import static net.minecraft.core.Direction.NORTH;
import static net.minecraft.core.Direction.SOUTH;
import static net.minecraft.core.Direction.UP;
import static net.minecraft.core.Direction.WEST;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import io.vram.frex.api.math.PackedSectionPos;
import io.vram.frex.base.renderer.mesh.BaseQuadView;

/**
 * Adapted from vanilla.
 */
enum AoFace {
	AOF_DOWN(WEST, EAST, NORTH, SOUTH,
		(q, i) -> clamp(q.y(i)),
		(q, i) -> clamp(q.z(i)),
		(q, i) -> 1 - clamp(q.x(i)),
		(q, i, w) -> {
			final float u = clamp(q.z(i));
			final float v = 1 - clamp(q.x(i));
			w[0] = v * u;
			w[1] = v * (1 - u);
			w[2] = (1 - v) * (1 - u);
			w[3] = (1 - v) * u;
		}),

	AOF_UP(EAST, WEST, NORTH, SOUTH,
		(q, i) -> 1 - clamp(q.y(i)),
		(q, i) -> clamp(q.z(i)),
		(q, i) -> clamp(q.x(i)),
		(q, i, w) -> {
			final float u = clamp(q.z(i));
			final float v = clamp(q.x(i));
			w[0] = v * u;
			w[1] = v * (1 - u);
			w[2] = (1 - v) * (1 - u);
			w[3] = (1 - v) * u;
		}),

	AOF_NORTH(UP, DOWN, EAST, WEST,
		(q, i) -> clamp(q.z(i)),
		(q, i) -> 1 - clamp(q.x(i)),
		(q, i) -> clamp(q.y(i)),
		(q, i, w) -> {
			final float u = 1 - clamp(q.x(i));
			final float v = clamp(q.y(i));
			w[0] = v * u;
			w[1] = v * (1 - u);
			w[2] = (1 - v) * (1 - u);
			w[3] = (1 - v) * u;
		}),
	AOF_SOUTH(WEST, EAST, DOWN, UP,
		(q, i) -> 1 - clamp(q.z(i)),
		(q, i) -> clamp(q.y(i)),
		(q, i) -> 1 - clamp(q.x(i)),
		(q, i, w) -> {
			final float u = clamp(q.y(i));
			final float v = 1 - clamp(q.x(i));
			w[0] = u * v;
			w[1] = (1 - u) * v;
			w[2] = (1 - u) * (1 - v);
			w[3] = u * (1 - v);
		}),
	AOF_WEST(UP, DOWN, NORTH, SOUTH,
		(q, i) -> clamp(q.x(i)),
		(q, i) -> clamp(q.z(i)),
		(q, i) -> clamp(q.y(i)),
		(q, i, w) -> {
			final float u = clamp(q.z(i));
			final float v = clamp(q.y(i));
			w[0] = v * u;
			w[1] = v * (1 - u);
			w[2] = (1 - v) * (1 - u);
			w[3] = (1 - v) * u;
		}),
	AOF_EAST(DOWN, UP, NORTH, SOUTH,
		(q, i) -> 1 - clamp(q.x(i)),
		(q, i) -> clamp(q.z(i)),
		(q, i) -> 1 - clamp(q.y(i)),
		(q, i, w) -> {
			final float u = clamp(q.z(i));
			final float v = 1 - clamp(q.y(i));
			w[0] = v * u;
			w[1] = v * (1 - u);
			w[2] = (1 - v) * (1 - u);
			w[3] = (1 - v) * u;
		});

	private static final AoFace[] values = createValues();
	final int[] neighbors;
	final WeightFunction weightFunc;
	final Vertex2Float depthFunc;
	final Vertex2Float uFunc;
	final Vertex2Float vFunc;

	final int bottomOffset;
	final int leftOffset;
	final int topOffset;
	final int rightOffset;
	final int bottomLeftOffset;
	final int bottomRightOffset;
	final int topLeftOffset;
	final int topRightOffset;

	AoFace(Direction bottom, Direction top, Direction left, Direction right, Vertex2Float depthFunc, Vertex2Float uFunc, Vertex2Float vFunc, WeightFunction weightFunc) {
		neighbors = new int[4];
		neighbors[0] = bottom.ordinal();
		neighbors[1] = top.ordinal();
		neighbors[2] = left.ordinal();
		neighbors[3] = right.ordinal();

		final Vec3i bottomVec = bottom.getNormal();
		final Vec3i leftVec = left.getNormal();
		final Vec3i topVec = top.getNormal();
		final Vec3i rightVec = right.getNormal();

		bottomOffset = PackedSectionPos.pack(bottomVec);
		leftOffset = PackedSectionPos.pack(leftVec);
		topOffset = PackedSectionPos.pack(topVec);
		rightOffset = PackedSectionPos.pack(rightVec);

		bottomLeftOffset = PackedSectionPos.add(bottomOffset, leftOffset);
		bottomRightOffset = PackedSectionPos.add(bottomOffset, rightOffset);
		topLeftOffset = PackedSectionPos.add(topOffset, leftOffset);
		topRightOffset = PackedSectionPos.add(topOffset, rightOffset);

		this.depthFunc = depthFunc;
		this.weightFunc = weightFunc;
		this.vFunc = vFunc;
		this.uFunc = uFunc;
	}

	private static AoFace[] createValues() {
		final AoFace[] result = new AoFace[6];
		result[DOWN.get3DDataValue()] = AOF_DOWN;
		result[UP.get3DDataValue()] = AOF_UP;
		result[NORTH.get3DDataValue()] = AOF_NORTH;
		result[SOUTH.get3DDataValue()] = AOF_SOUTH;
		result[WEST.get3DDataValue()] = AOF_WEST;
		result[EAST.get3DDataValue()] = AOF_EAST;
		return result;
	}

	public static AoFace get(int directionOrdinal) {
		return values[directionOrdinal];
	}

	/**
	 * Implementations handle bilinear interpolation of a point on a light face by
	 * computing weights for each corner of the light face. Relies on the fact that
	 * each face is a unit cube. Uses coordinates from axes orthogonal to face as
	 * distance from the edge of the cube, flipping as needed. Multiplying distance
	 * coordinate pairs together gives sub-area that are the corner weights. Weights
	 * sum to 1 because it is a unit cube. Values are stored in the provided array.
	 */
	@FunctionalInterface
	interface WeightFunction {
		void apply(BaseQuadView q, int vertexIndex, float[] out);
	}

	@FunctionalInterface
	interface Vertex2Float {
		float apply(BaseQuadView q, int vertexIndex);
	}
}
