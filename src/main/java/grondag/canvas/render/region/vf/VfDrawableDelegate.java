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

package grondag.canvas.render.region.vf;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.region.base.AbstractDrawableDelegate;
import grondag.canvas.vf.storage.VfStorageReference;

public final class VfDrawableDelegate extends AbstractDrawableDelegate {
	private VfStorageReference regionStorageReference;

	public VfDrawableDelegate(RenderState renderState, int quadVertexCount, VfStorageReference regionStorageReference) {
		super(renderState, quadVertexCount);
		this.regionStorageReference = regionStorageReference;
	}

	public VfStorageReference regionStorageReference() {
		return regionStorageReference;
	}

	@Override
	public void closeInner() {
		if (regionStorageReference != null) {
			regionStorageReference.close();
			regionStorageReference = null;
		}
	}
}
