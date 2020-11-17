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

import grondag.canvas.buffer.encoding.CanvasImmediate;
import grondag.canvas.mixinterface.BufferBuilderStorageExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;

@Mixin(BufferBuilderStorage.class)
public class MixinBufferBuilderStorage implements BufferBuilderStorageExt {
	@Shadow private Immediate entityVertexConsumers;

	private Immediate activeEntityVertexConsumers = entityVertexConsumers;

	/**
	 * @reason simple and reliable
	 */
	@Overwrite
	public VertexConsumerProvider.Immediate getEntityVertexConsumers() {
		return activeEntityVertexConsumers;
	}

	@Override
	public void canvas_setEntityConsumers(CanvasImmediate consumers) {
		activeEntityVertexConsumers = consumers;
	}
}
