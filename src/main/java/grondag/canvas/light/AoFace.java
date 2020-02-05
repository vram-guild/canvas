/*******************************************************************************
 * Copyright 2019, 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package grondag.canvas.light;

import static grondag.canvas.light.AoVertexClampFunction.clamp;
import static net.minecraft.util.math.Direction.DOWN;
import static net.minecraft.util.math.Direction.EAST;
import static net.minecraft.util.math.Direction.NORTH;
import static net.minecraft.util.math.Direction.SOUTH;
import static net.minecraft.util.math.Direction.UP;
import static net.minecraft.util.math.Direction.WEST;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.apiimpl.mesh.QuadViewImpl;

/**
 * Adapted from vanilla BlockModelRenderer.AoCalculator.
 */
@Environment(EnvType.CLIENT)
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

	final int[] neighbors;
	final WeightFunction weightFunc;
	final Vertex2Float depthFunc;
	final Vertex2Float uFunc;
	final Vertex2Float vFunc;
	final Vec3i bottomVec;
	final Vec3i leftVec;
	final Vec3i topVec;
	final Vec3i rightVec;
	final Vec3i bottomLeftVec;
	final Vec3i bottomRightVec;
	final Vec3i topLeftVec;
	final Vec3i topRightVec;

	private AoFace(Direction bottom, Direction top, Direction left, Direction right, Vertex2Float depthFunc,
			Vertex2Float uFunc, Vertex2Float vFunc, WeightFunction weightFunc) {
		neighbors = new int[4];
		neighbors[0] = bottom.ordinal();
		neighbors[1] = top.ordinal();
		neighbors[2] = left.ordinal();
		neighbors[3] = right.ordinal();

		bottomVec = bottom.getVector();
		leftVec = left.getVector();
		topVec = top.getVector();
		rightVec = right.getVector();

		bottomLeftVec = new Vec3i(bottomVec.getX() + leftVec.getX(), bottomVec.getY() + leftVec.getY(), bottomVec.getZ() + leftVec.getZ());
		bottomRightVec = new Vec3i(bottomVec.getX() + rightVec.getX(), bottomVec.getY() + rightVec.getY(), bottomVec.getZ() + rightVec.getZ());
		topLeftVec = new Vec3i(topVec.getX() + leftVec.getX(), topVec.getY() + leftVec.getY(), topVec.getZ() + leftVec.getZ());
		topRightVec = new Vec3i(topVec.getX() + rightVec.getX(), topVec.getY() + rightVec.getY(), topVec.getZ() + rightVec.getZ());

		this.depthFunc = depthFunc;
		this.weightFunc = weightFunc;
		this.vFunc = vFunc;
		this.uFunc = uFunc;
	}

	private static final AoFace[] values = createValues();

	private static AoFace[] createValues() {
		final AoFace[] result = new AoFace[6];
		result[DOWN.getId()] = AOF_DOWN;
		result[UP.getId()] = AOF_UP;
		result[NORTH.getId()] = AOF_NORTH;
		result[SOUTH.getId()] = AOF_SOUTH;
		result[WEST.getId()] = AOF_WEST;
		result[EAST.getId()] = AOF_EAST;
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
	static interface WeightFunction {
		void apply(QuadViewImpl q, int vertexIndex, float[] out);
	}

	@FunctionalInterface
	static interface Vertex2Float {
		float apply(QuadViewImpl q, int vertexIndex);
	}
}
