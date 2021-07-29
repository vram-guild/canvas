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

import grondag.canvas.render.terrain.TerrainRenderConfig;
import grondag.canvas.render.terrain.cluster.ClusteredRegionRenderConfig;
import grondag.canvas.render.terrain.vbo.VboRegionRenderConfig;

public enum TerrainRenderConfigOption {
	DEFAULT(VboRegionRenderConfig.INSTANCE),
	CLUSTERED(ClusteredRegionRenderConfig.INSTANCE);

	TerrainRenderConfigOption(TerrainRenderConfig config) {
		this.config = config;
	}

	public final TerrainRenderConfig config;
}
