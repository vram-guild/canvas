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

package grondag.canvas.terrain.occlusion.camera;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.CanvasMod;
import grondag.canvas.terrain.occlusion.base.PotentiallyVisibleRegionSet;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.terrain.region.RenderRegionIndexer;

/**
 * Sorts render regions by distance in a single array using
 * direct addressing. Exploits the fact chunks will always
 * be at a finite number of distances from the origin chunk
 * and slots them into buckets using simple and fast array access.
 */
public class CameraPotentiallyVisibleRegionSet implements PotentiallyVisibleRegionSet<CameraPotentiallyVisibleRegionSet, CameraRegionVisibility> {
	/**
	 * The number of unique squared distance values ("rings") that occur
	 * in our voxelized sphere.
	 */
	private static final int RING_COUNT;

	/**
	 * Max squared chunk distance that occurs. To allow for validity checks.
	 */
	private static final int MAX_SQ_DIST;

	/**
	 * Length of {@link #SQ_DIST_TO_RING_MAP}. Will be +1 because we
	 * include both zero and the max distance.
	 */
	private static final int RING_MAP_LENGTH;

	/**
	 * Array index is squared chunk distance, values are the first index
	 * location to store that distance.  Value will be maxint for squared
	 * distance values that should not occur. (Maxint helps assertion.)
	 */
	private static final int[] SQ_DIST_TO_RING_MAP;

	/**
	 * Size of array needed to store regions sorted by distance. Is half
	 * the total number of positions in a 34-radius voxelized sphere because
	 * only half of the sphere can be visible at a time.
	 */
	private static final int REGION_LOOKUP_LENGTH;

	static {
		final Int2IntOpenHashMap rings = new Int2IntOpenHashMap();

		// 34 to allow for padding
		for (int x = -RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS; x <= RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS; ++x) {
			for (int z = -RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS; z <= RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS; ++z) {
				// not clamped on Y because player can be above or below world
				for (int y = -RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS; y <= RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS; ++y) {
					final int d = x * x + y * y + z * z;
					rings.addTo(d, 1);
				}
			}
		}

		final int[] dist = rings.keySet().toIntArray();
		Arrays.sort(dist);

		RING_COUNT = dist.length;
		MAX_SQ_DIST = dist[RING_COUNT -1];
		RING_MAP_LENGTH = MAX_SQ_DIST + 1;
		SQ_DIST_TO_RING_MAP = new int[RING_MAP_LENGTH];
		Arrays.fill(SQ_DIST_TO_RING_MAP, Integer.MAX_VALUE);

		int index = 0;

		for (final int k : dist) {
			SQ_DIST_TO_RING_MAP[k] = index;
			index += rings.get(k);
		}

		REGION_LOOKUP_LENGTH = index;
	}

	private int version = 1;

	@Override
	public int version() {
		return version;
	}

	/**
	 * Same as {@link #SQ_DIST_TO_RING_MAP} but indices are updated as we collect regions.
	 * Starting state is a copy of {@link #SQ_DIST_TO_RING_MAP}.
	 */
	private final int[] ringMap = new int[RING_MAP_LENGTH];
	private final CameraRegionVisibility[] states = new CameraRegionVisibility[REGION_LOOKUP_LENGTH];

	private int iterationIndex = 0;
	private int maxIndex = 0;

	public CameraPotentiallyVisibleRegionSet() {
		clear();
	}

	@Override
	public void clear() {
		System.arraycopy(SQ_DIST_TO_RING_MAP, 0, ringMap, 0, RING_MAP_LENGTH);
		Arrays.fill(states, null);
		maxIndex = -1;
		++version;
		returnToStart();
	}

	@Override
	public void returnToStart() {
		iterationIndex = 0;
	}

	@Override
	public void add(CameraRegionVisibility state) {
		final RenderRegion region = state.region;
		final int dist = region.origin.squaredCameraChunkDistance();

		if (dist >= 0 && dist <= MAX_SQ_DIST) {
			final int index = ringMap[dist];
			assert isSaneAddition(region, dist, index) : "Region ring index overrun into next (more distant) region.";
			assert index >= iterationIndex || region.origin.isNear() || !region.getBuildState().canOcclude() : "Region added before PVS iteration pointer";

			states[index] = state;
			ringMap[dist] = index + 1;

			if (index > maxIndex) {
				maxIndex = index;
			}
		}
	}

	private boolean isSaneAddition(RenderRegion region, int dist, int targetIndex) {
		final int limit = SQ_DIST_TO_RING_MAP[dist + 1];

		if (dist < MAX_SQ_DIST && targetIndex >= limit) {
			CanvasMod.LOG.info("Origin region: " + (states[0] == null ? "null" : states[0].toString()));
			CanvasMod.LOG.info("Region to be added: " + region.toString());
			CanvasMod.LOG.info("Regions extant in same ring follow...");

			for (int i = SQ_DIST_TO_RING_MAP[dist]; i < limit; ++i) {
				CanvasMod.LOG.info(states[i].toString());
			}

			return false;
		} else {
			return true;
		}
	}

	@Override
	@Nullable public CameraRegionVisibility next() {
		final int maxIndex = this.maxIndex;

		while (iterationIndex <= maxIndex) {
			final CameraRegionVisibility state = states[iterationIndex++];

			if (state != null) {
				return state;
			}
		}

		return null;
	}
}
