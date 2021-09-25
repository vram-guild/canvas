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

package grondag.canvas.apiimpl.rendercontext;

import com.mojang.math.Matrix4f;

import grondag.canvas.buffer.format.EncodingContext;
import grondag.canvas.mixinterface.Matrix3fExt;

public abstract class AbstractEncodingContext implements EncodingContext {
	/** Used by some terrain render configs to pass a region ID into vertex encoding. */
	public int sectorId;
	public int sectorRelativeRegionOrigin;
	protected Matrix4f matrix;
	protected Matrix3fExt normalMatrix;
	protected int overlay;

	@Override
	public final int overlay() {
		return overlay;
	}

	@Override
	public final Matrix4f matrix() {
		return matrix;
	}

	@Override
	public final Matrix3fExt normalMatrix() {
		return normalMatrix;
	}

	@Override
	public final int sectorId() {
		return sectorId;
	}

	@Override
	public final int sectorRelativeRegionOrigin() {
		return sectorRelativeRegionOrigin;
	}
}
