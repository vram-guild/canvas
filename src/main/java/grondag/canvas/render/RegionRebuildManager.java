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

package grondag.canvas.render;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.util.Util;

import grondag.canvas.terrain.region.BuiltRenderRegion;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;

/**
 * Tracks what regions require rebuilding and rebuilds them on demand.
 *
 * <p>Handles regions for both camera and shadow views.  "Near" regions
 * are exclusively a camera-view concept.
 */
public class RegionRebuildManager {
	private final Set<BuiltRenderRegion> regionsToRebuild = Sets.newLinkedHashSet();

	/**
	 * Iterates the given list of regions and if a region requires an urgent
	 * rebuild or is near the camera, immediately rebuilds it on the calling
	 * thread (which should always be the render thread), removing the region
	 * from the set of regions scheduled for rebuild.
	 *
	 * <p>Otherwise the region is scheduled for rebuild off thread, unless it
	 * does not require rebuild. (It may have completed rebuild off-thread because
	 * it was scheduled earlier.)
	 *
	 * <p>If a region reports it does not require rebuild or is already scheduled
	 * for rebuild, effectively nothing happens for that region.
	 *
	 * <p>Not thread-safe and meant to be called from the main render thread.
	 *
	 * @param updateRegions list of regions potentially needing rebuilt
	 */
	void scheduleOrBuild(SimpleUnorderedArrayList<BuiltRenderRegion> updateRegions) {
		final int limit = updateRegions.size();
		final Set<BuiltRenderRegion> regionsToRebuild = this.regionsToRebuild;

		if (limit == 0) {
			return;
		}

		for (int i = 0; i < limit; ++i) {
			final BuiltRenderRegion region = updateRegions.get(i);

			if (region.needsRebuild()) {
				if (region.needsImportantRebuild() || region.isNear()) {
					regionsToRebuild.remove(region);
					region.rebuildOnMainThread();
				} else {
					regionsToRebuild.add(region);
				}
			}
		}
	}

	/**
	 * Rebuilds the region on the calling thread if needed,
	 * also removing it from the set of regions awaiting off-thread
	 * rebuild if it was present.
	 *
	 * <p>Not thread-safe and meant to be called from the main render thread.
	 *
	 * @param region Region to be checked and rebuilt.
	 */
	void buildNearRegionIfNeeded(BuiltRenderRegion region) {
		if (region.needsRebuild()) {
			regionsToRebuild.remove(region);
			region.rebuildOnMainThread();
		}
	}

	/**
	 * Iterates regions awaiting rebuild and rebuilds them on the calling
	 * thread if they are urgent/near.  For non-urgent regions, necessary
	 * world data are captured on the calling thread and packaged into a
	 * rebuild task that runs off thread.
	 *
	 * <p>Not thread-safe and meant to be called from the main render thread.
	 *
	 * @param endNanos The max end time for this task. (NOT the duration.)
	 * Processing will end when the system nanotime exceeds this value.
	 */
	void processScheduledRegions(long endNanos) {
		final Set<BuiltRenderRegion> regionsToRebuild = this.regionsToRebuild;

		//final long start = Util.getMeasuringTimeNano();
		//int builtCount = 0;

		if (!regionsToRebuild.isEmpty()) {
			final Iterator<BuiltRenderRegion> iterator = regionsToRebuild.iterator();

			while (iterator.hasNext()) {
				final BuiltRenderRegion builtRegion = iterator.next();

				if (builtRegion.needsImportantRebuild()) {
					builtRegion.rebuildOnMainThread();
				} else {
					builtRegion.prepareAndExecuteRebuildTask();
				}

				iterator.remove();

				// this seemed excessive
				//				++builtCount;
				//
				//				final long now = Util.getMeasuringTimeNano();
				//				final long elapsed = now - start;
				//				final long avg = elapsed / builtCount;
				//				final long remaining = endNanos - now;
				//
				//				if (remaining < avg) {
				//					break;
				//				}

				if (Util.getMeasuringTimeNano() >= endNanos) {
					break;
				}
			}
		}
	}

	public void clear() {
		regionsToRebuild.clear();
	}

	public boolean isEmpty() {
		return regionsToRebuild.isEmpty();
	}
}
