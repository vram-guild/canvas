/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.mixin;

import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.mixinterface.Matrix3fExt;
import net.minecraft.util.math.Matrix3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix3f.class)
public class MixinMatrix3f implements Matrix3fExt {
	@Shadow
	protected float a00;
	@Shadow
	protected float a01;
	@Shadow
	protected float a02;
	@Shadow
	protected float a10;
	@Shadow
	protected float a11;
	@Shadow
	protected float a12;
	@Shadow
	protected float a20;
	@Shadow
	protected float a21;
	@Shadow
	protected float a22;

	@Override
	public int canvas_transform(int packedNormal) {
		final float x = NormalHelper.getPackedNormalComponent(packedNormal, 0);
		final float y = NormalHelper.getPackedNormalComponent(packedNormal, 1);
		final float z = NormalHelper.getPackedNormalComponent(packedNormal, 2);
		final float w = NormalHelper.getPackedNormalComponent(packedNormal, 3);

		final float nx = a00 * x + a01 * y + a02 * z;
		final float ny = a10 * x + a11 * y + a12 * z;
		final float nz = a20 * x + a21 * y + a22 * z;

		return NormalHelper.packNormal(nx, ny, nz, w);
	}
}
