/*******************************************************************************
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
 ******************************************************************************/

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.chunk.ProtoRenderRegion;
import grondag.canvas.chunk.RebuildTaskFactory;
import grondag.canvas.mixinterface.AccessRebuildTask;

@Mixin(BuiltChunk.class)
public abstract class MixinBuiltChunk {
	@Shadow private BlockPos.Mutable origin;
	@Shadow protected abstract void cancel();
	@Shadow protected abstract double getSquaredCameraDistance();
	@Shadow protected ChunkBuilder field_20833;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Inject(method = "createRebuildTask", at = { @At(value = "HEAD") }, cancellable = true)
	private void hookCreateRebuildTask(CallbackInfoReturnable ci) {
		cancel();
		final BlockPos blockPos = origin.toImmutable();
		final Object rebuildTask = RebuildTaskFactory.INSTANCE.get((BuiltChunk)(Object) this, getSquaredCameraDistance());
		((AccessRebuildTask) rebuildTask).canvas_setRegion(ProtoRenderRegion.claim(((AccessChunkBuilder) field_20833).getWorld(), blockPos));
		ci.setReturnValue(rebuildTask);
	}
}
