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

package grondag.canvas.render.terrain.cluster;

import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.render.terrain.base.DrawableRegion;
import grondag.canvas.render.terrain.base.UploadableRegion;

public class ClusteredUploadableRegion implements UploadableRegion {
	protected final DrawableRegion drawable;

	public ClusteredUploadableRegion(VertexCollectorList collectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
		drawable = ClusteredDrawableRegion.pack(collectorList, sorted, bytes, packedOriginBlockPos);
	}

	@Override
	public DrawableRegion produceDrawable() {
		drawable.drawState().storage().upload();
		return drawable;
	}
}
