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

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.mixinterface.Matrix3fExt;

public final class AbsentEncodingContext extends AbstractEncodingContext {
	private AbsentEncodingContext() {
		matrix = new Matrix4f();
		matrix.loadIdentity();
		overlay = OverlayTexture.DEFAULT_UV;

		final Matrix3f n = new Matrix3f();
		n.loadIdentity();
		normalMatrix = (Matrix3fExt) (Object) n;
	}

	public static final AbsentEncodingContext INSTANCE = new AbsentEncodingContext();
}
