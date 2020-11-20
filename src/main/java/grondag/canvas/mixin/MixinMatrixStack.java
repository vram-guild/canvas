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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.util.math.MatrixStack;

import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.mixinterface.MatrixStackExt;
import grondag.canvas.varia.MatrixStackEntryHelper;

@Mixin(MatrixStack.class)
public class MixinMatrixStack implements MatrixStackExt {
	@Shadow private Deque<MatrixStack.Entry> stack;

	private final ObjectArrayList<MatrixStack.Entry> pool = new ObjectArrayList<>();

	/**
	 * @author grondag
	 * @reason performance
	 */
	@Overwrite
	public void pop() {
		pool.add(stack.removeLast());
	}

	/**
	 * @author grondag
	 * @reason performance
	 */
	@Overwrite
	public void push() {
		final MatrixStack.Entry current = stack.getLast();
		MatrixStack.Entry add;

		if (pool.isEmpty()) {
			add = MatrixStackEntryHelper.create(current.getModel().copy(), current.getNormal().copy());
		} else {
			add = pool.pop();
			((Matrix4fExt) (Object) add.getModel()).set(current.getModel());
			((Matrix3fExt) (Object) add.getNormal()).set(current.getNormal());
		}

		stack.addLast(add);
	}

	@Override
	public int canvas_size() {
		return stack.size();
	}
}
