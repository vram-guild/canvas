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
