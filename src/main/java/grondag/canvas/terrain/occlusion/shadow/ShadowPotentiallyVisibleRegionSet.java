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

package grondag.canvas.terrain.occlusion.shadow;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;

import io.vram.dtk.CircleUtil;

import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.terrain.occlusion.base.PotentiallyVisibleRegionSet;
import grondag.canvas.terrain.region.RenderRegionIndexer;

public class ShadowPotentiallyVisibleRegionSet implements PotentiallyVisibleRegionSet<ShadowPotentiallyVisibleRegionSet, ShadowRegionVisibility> {
	private int version = 1;
	private int regionCount = 0;
	private ShadowRegionVisibility[] states;

	int xBase;
	int zBase;

	private int x, y, z;

	private int maxYRegions;
	private int blockYOffset;

	public ShadowPotentiallyVisibleRegionSet() {
	}

	public void resetWorld(WorldRenderState worldRenderState) {
		final var world = worldRenderState.getWorld();

		if (world == null) {
			return;
		}

		// TODO: do something about this hard limit (see rankIndex implementation)
		maxYRegions = Math.min(RenderRegionIndexer.maxYRegions(world), RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER);
		blockYOffset = RenderRegionIndexer.blockYOffset(world);

		final int maxStates = RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT * maxYRegions;

		if (states == null || states.length < maxStates) {
			states = new ShadowRegionVisibility[maxStates];
		}
	}

	private DirectionFunction xDir = DIRECTION_NORMAL, yDir = DIRECTION_NORMAL, zDir = DIRECTION_NORMAL;

	private final AxisIterator XPOS = new AxisIterator() {
		@Override
		public boolean next() {
			if (++x < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			x = 0;
		}
	};

	private final AxisIterator XNEG = new AxisIterator() {
		@Override
		public boolean next() {
			if (--x >= 0) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			x = RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER - 1;
		}
	};

	private final AxisIterator YPOS = new AxisIterator() {
		@Override
		public boolean next() {
			if (++y < maxYRegions) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			y = 0;
		}
	};

	private final AxisIterator YNEG = new AxisIterator() {
		@Override
		public boolean next() {
			if (--y >= 0) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			y = maxYRegions - 1;
		}
	};

	private final AxisIterator ZPOS = new AxisIterator() {
		@Override
		public boolean next() {
			if (++z < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			z = 0;
		}
	};

	private final AxisIterator ZNEG = new AxisIterator() {
		@Override
		public boolean next() {
			if (--z >= 0) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			z = RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER - 1;
		}
	};

	private AxisIterator primary = XPOS;
	private AxisIterator secondary = YPOS;
	private AxisIterator tertiary = ZPOS;

	private DistanceRankFunction distanceRankFunction = RANK_XYZ;

	// Lightweight mutable vec3i alternative
	private final int[] searcher = new int[3];

	private int[] searcher(int x, int y, int z) {
		searcher[0] = x;
		searcher[1] = y;
		searcher[2] = z;
		return searcher;
	}

	// Aligns primed shadow region coords to the ends of the respective iteration axes, chunk multiplication included
	private final PrimerAligner ALIGN_XPOS = (off, rad) -> searcher(-(rad << 4), off.x() << 4, off.y() << 4);
	private final PrimerAligner ALIGN_XNEG = (off, rad) -> searcher((rad << 4), off.x() << 4, off.y() << 4);
	private final PrimerAligner ALIGN_YPOS = (off, rad) -> searcher(off.x() << 4, -(rad << 4), off.y() << 4);
	private final PrimerAligner ALIGN_YNEG = (off, rad) -> searcher(off.x() << 4, (rad << 4), off.y() << 4);
	private final PrimerAligner ALIGN_ZPOS = (off, rad) -> searcher(off.x() << 4, off.y() << 4, -(rad << 4));
	private final PrimerAligner ALIGN_ZNEG = (off, rad) -> searcher(off.x() << 4, off.y() << 4, (rad << 4));

	private PrimerAligner primerAligner = ALIGN_XPOS;

	public int[] alignPrimerCircle(CircleUtil.Offset circleOffset, int sphereRadius) {
		return primerAligner.getAligned(circleOffset, sphereRadius);
	}

	public void setLightVectorAndRestart(Vector3f vec) {
		setLightVectorAndRestart(vec.x(), vec.y(), vec.z());
	}

	/**
	 * Points toward the light from any point in the scene.
	 * (Assumes an orthogonal light/shadow projection.)
	 * Used to control the order of iteration and can be
	 * changed after regions are added.
	 *
	 * <p>Calls {@link #returnToStart()} because a new light
	 * vector invalidates the previous sort order.
	 *
	 * @param x x-axis component of light vector
	 * @param y y-axis component of light vector
	 * @param z z-axis component of light vector
	 */
	public void setLightVectorAndRestart(float x, float y, float z) {
		final float ax = Math.abs(x);
		final float ay = Math.abs(y);
		final float az = Math.abs(z);

		// Signs are flipped because farther from light = higher distance rank
		xDir = x < 0 ? DIRECTION_NORMAL : DIRECTION_FLIPPED;
		yDir = y < 0 ? DIRECTION_NORMAL : DIRECTION_FLIPPED;
		zDir = z < 0 ? DIRECTION_NORMAL : DIRECTION_FLIPPED;

		if (ax > ay) {
			if (ax > az) {
				// X primary
				primerAligner = x > 0 ? ALIGN_XNEG : ALIGN_XPOS;

				if (ay > az) {
					// ORDER XYZ
					distanceRankFunction = RANK_XYZ;
					primary = x > 0 ? XNEG : XPOS;
					secondary = y > 0 ? YNEG : YPOS;
					tertiary = z > 0 ? ZNEG : ZPOS;
				} else {
					// ORDER XZY
					distanceRankFunction = RANK_XZY;
					primary = x > 0 ? XNEG : XPOS;
					secondary = z > 0 ? ZNEG : ZPOS;
					tertiary = y > 0 ? YNEG : YPOS;
				}
			} else {
				// Z primary, because X > Y and Z >= X
				primerAligner = z > 0 ? ALIGN_ZNEG : ALIGN_ZPOS;
				// ORDER ZXY
				distanceRankFunction = RANK_ZXY;
				primary = z > 0 ? ZNEG : ZPOS;
				secondary = x > 0 ? XNEG : XPOS;
				tertiary = y > 0 ? YNEG : YPOS;
			}
		} else {
			// X cannot be primary
			if (ay > az) {
				// Y primary, XZ order undetermined
				primerAligner = y > 0 ? ALIGN_YNEG : ALIGN_YPOS;

				if (ax > az) {
					// ORDER YXZ
					distanceRankFunction = RANK_YXZ;
					primary = y > 0 ? YNEG : YPOS;
					secondary = x > 0 ? XNEG : XPOS;
					tertiary = z > 0 ? ZNEG : ZPOS;
				} else {
					// ORDER YZX
					distanceRankFunction = RANK_YZX;
					primary = y > 0 ? YNEG : YPOS;
					secondary = z > 0 ? ZNEG : ZPOS;
					tertiary = x > 0 ? XNEG : XPOS;
				}
			} else {
				// Z primary, because Y >= X and Z >= Y
				primerAligner = z > 0 ? ALIGN_ZNEG : ALIGN_ZPOS;
				// ORDER ZYX
				distanceRankFunction = RANK_ZYX;
				primary = z > 0 ? ZNEG : ZPOS;
				secondary = y > 0 ? YNEG : YPOS;
				tertiary = x > 0 ? XNEG : XPOS;
			}
		}

		returnToStart();
	}

	/**
	 * Captures the x and z origin coordinates of the camera region.
	 * This is used to compute a relative position for iteration.
	 * The y coordinate is not used because the sky light is always
	 * outside of the world and y isn't useful as a relative origin.
	 *
	 * <p>Also calls {@link #clear()} because changing the origin
	 * invalidates the addressing of any regions already added.
	 *
	 * @param x x-axis block position coordinate of the camera region/chunk origin
	 * @param z z-axis block position coordinate of the camera region/chunk origin
	 */
	public void setCameraChunkOriginAndClear(int x, int z) {
		xBase = RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS - (x >> 4);
		zBase = RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS - (z >> 4);
		clear();
	}

	@Override
	public int version() {
		return version;
	}

	@Override
	public void clear() {
		assert states != null;
		Arrays.fill(states, null);
		regionCount = 0;
		++version;
		returnToStart();
	}

	@Override
	public void add(ShadowRegionVisibility state) {
		assert states != null;

		final BlockPos origin = state.region.origin;
		final int rx = (origin.getX() >> 4) + xBase;
		final int rz = (origin.getZ() >> 4) + zBase;
		final int ry = (origin.getY() + blockYOffset) >> 4;

		//System.out.println(String.format("Adding origin %s with region pos %d  %d  %d  with index %d", region.origin().toShortString(), rx, ry, rz, index(rx, ry, rz)));
		final int i = index(rx, ry, rz);
		assert states[i] == null;
		states[i] = state;
		++regionCount;
	}

	boolean complete = false;

	public int regionCount() {
		return regionCount;
	}

	@Override
	public void returnToStart() {
		primary.reset();
		secondary.reset();
		tertiary.reset();
		complete = false;
	}

	@Override
	public @Nullable ShadowRegionVisibility next() {
		assert states != null;

		ShadowRegionVisibility result = null;

		if (!complete) {
			while (result == null) {
				final int i = index(x, y, z);
				result = states[i];

				if (!tertiary.next()) {
					if (!secondary.next()) {
						if (!primary.next()) {
							complete = true;
							break;
						}
					}
				}
			}
		}

		return result;
	}

	public int distanceRank(ShadowRegionVisibility state) {
		final BlockPos origin = state.region.origin;
		final int rx = (origin.getX() >> 4) + xBase;
		final int rz = (origin.getZ() >> 4) + zBase;
		final int ry = (origin.getY() + blockYOffset) >> 4;
		return distanceRankFunction.distanceRank(xDir.apply(rx), yDir.apply(ry), zDir.apply(rz));
	}

	/**
	 * Computes index given normalized x, y, z region coordinates.
	 *
	 * <p>These are chunk-type coordinates, not block coordinates. (>> 4).
	 *
	 * @param rx chunk coordinate relative to xBase (0 to MAX_CHUNK_DIAMETER - 1)
	 * @param ry chunk coordinate relative to Y_BLOCKPOS_OFFSET
	 * @param rz chunk coordinate relative to zBase (0 to MAX_CHUNK_DIAMETER - 1)
	 * @return index to region array, will be within {@link RenderRegionIndexer#MAX_LOADED_CHUNK_DIAMETER}
	 */
	private int index(int rx, int ry, int rz) {
		assert ry < maxYRegions;
		return rankIndex(ry, rz, rx);
	}

	private static int rankIndex(int primary, int secondary, int tertiary) {
		assert primary >= 0;
		assert primary <= RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER;
		assert secondary >= 0;
		assert secondary <= RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER;
		assert tertiary >= 0;
		assert tertiary <= RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER;

		return tertiary | (secondary << RenderRegionIndexer.CHUNK_DIAMETER_BITS) | (primary << (RenderRegionIndexer.CHUNK_DIAMETER_BITS * 2));
	}

	public int primary(int shadowDistanceRank) {
		return shadowDistanceRank >> (RenderRegionIndexer.CHUNK_DIAMETER_BITS * 2);
	}

	private interface AxisIterator {
		boolean next();

		void reset();
	}

	private interface DirectionFunction {
		int apply(int val);
	}

	private static final DirectionFunction DIRECTION_NORMAL = n -> n;
	private static final DirectionFunction DIRECTION_FLIPPED = n -> RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER - n;

	private interface DistanceRankFunction {
		int distanceRank(int x, int y, int z);
	}

	private static final DistanceRankFunction RANK_XYZ = (x, y, z) -> rankIndex(x, y, z);
	private static final DistanceRankFunction RANK_XZY = (x, y, z) -> rankIndex(x, z, y);
	private static final DistanceRankFunction RANK_YXZ = (x, y, z) -> rankIndex(y, x, z);
	private static final DistanceRankFunction RANK_YZX = (x, y, z) -> rankIndex(y, z, x);
	private static final DistanceRankFunction RANK_ZXY = (x, y, z) -> rankIndex(z, x, y);
	private static final DistanceRankFunction RANK_ZYX = (x, y, z) -> rankIndex(z, y, x);

	private interface PrimerAligner {
		int[] getAligned(CircleUtil.Offset circleOffset, int sphereRadius);
	}
}
