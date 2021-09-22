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

package grondag.canvas.mixin;

import java.util.Deque;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.mixinterface.PoseStackExt;
import grondag.canvas.varia.MatrixStackEntryHelper;

@Mixin(PoseStack.class)
public class MixinPoseStack implements PoseStackExt {
	@Shadow private Deque<PoseStack.Pose> poseStack;

	private final ObjectArrayList<PoseStack.Pose> pool = new ObjectArrayList<>();

	/**
	 * @author grondag
	 * @reason performance
	 */
	@Overwrite
	public void popPose() {
		pool.add(poseStack.removeLast());
	}

	/**
	 * @author grondag
	 * @reason performance
	 */
	@Overwrite
	public void pushPose() {
		final PoseStack.Pose current = poseStack.getLast();
		PoseStack.Pose add;

		if (pool.isEmpty()) {
			add = MatrixStackEntryHelper.create(current.pose().copy(), current.normal().copy());
		} else {
			add = pool.pop();
			((Matrix4fExt) (Object) add.pose()).set(current.pose());
			((Matrix3fExt) (Object) add.normal()).set(current.normal());
		}

		poseStack.addLast(add);
	}

	@Override
	public int canvas_size() {
		return poseStack.size();
	}
}
