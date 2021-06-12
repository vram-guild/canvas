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

public class RegionRebuildManager {
	private final Set<BuiltRenderRegion> regionsToRebuild = Sets.newLinkedHashSet();

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
					region.markBuilt();
				} else {
					regionsToRebuild.add(region);
				}
			}
		}
	}

	void buildNearRegionIfNeeded(BuiltRenderRegion region) {
		if (region.needsRebuild()) {
			regionsToRebuild.remove(region);
			region.rebuildOnMainThread();
			region.markBuilt();
		}
	}

	void updateRegions(long endNanos) {
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
					builtRegion.scheduleRebuild();
				}

				builtRegion.markBuilt();
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
