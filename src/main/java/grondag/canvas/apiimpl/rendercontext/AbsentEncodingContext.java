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

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.renderer.texture.OverlayTexture;

import io.vram.frex.api.math.FastMatrix3f;

public final class AbsentEncodingContext extends AbstractEncodingContext {
	private AbsentEncodingContext() {
		matrix = new Matrix4f();
		matrix.setIdentity();
		overlay = OverlayTexture.NO_OVERLAY;

		final Matrix3f n = new Matrix3f();
		n.setIdentity();
		normalMatrix = (FastMatrix3f) (Object) n;
	}

	public static final AbsentEncodingContext INSTANCE = new AbsentEncodingContext();
}
