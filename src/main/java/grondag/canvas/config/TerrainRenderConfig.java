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

package grondag.canvas.config;

import grondag.canvas.render.region.base.RegionRenderConfig;
import grondag.canvas.render.region.vbo.VboRegionRenderConfig;
import grondag.canvas.render.region.vf.VfRegionRenderConfig;
import grondag.canvas.render.region.vs.ClumpedRegionRenderConfig;
import grondag.canvas.render.region.vs.MultiClumpedRegionRenderConfig;

public enum TerrainRenderConfig {
	DEFAULT(VboRegionRenderConfig.INSTANCE),
	FETCH(VfRegionRenderConfig.INSTANCE),
	//REGION(NaiveVsRegionRenderConfig.INSTANCE);
	//REGION(NaiveVsRegionRenderConfig2.INSTANCE);
	REGION(ClumpedRegionRenderConfig.INSTANCE),
	MULTI(MultiClumpedRegionRenderConfig.INSTANCE);
	//REGION(SimpleVsRegionRenderConfig.INSTANCE);

	TerrainRenderConfig(RegionRenderConfig config) {
		this.config = config;
	}

	public final RegionRenderConfig config;
}