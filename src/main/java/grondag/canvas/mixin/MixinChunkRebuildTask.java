/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
/*
 * Copyright (c) 2020 Grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grondag.canvas.mixin;

import java.util.Set;

import com.google.common.collect.Sets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.chunk.ChunkRebuildinator;
import grondag.canvas.chunk.OcclusionRegion;
import grondag.canvas.chunk.ProtoRenderRegion;
import grondag.canvas.mixinterface.AccessChunkRendererData;
import grondag.canvas.mixinterface.AccessRebuildTask;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk$RebuildTask")
public abstract class MixinChunkRebuildTask implements AccessRebuildTask {
	@Shadow protected BuiltChunk field_20839;
	private ProtoRenderRegion protoRegion;
	@Shadow private <E extends BlockEntity> void addBlockEntity(ChunkBuilder.ChunkData chunkData, Set<BlockEntity> set, E blockEntity) {}

	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask;render(FFFLnet/minecraft/client/render/chunk/ChunkBuilder$ChunkData;Lnet/minecraft/client/render/chunk/BlockBufferBuilderStorage;)Ljava/util/Set;", cancellable = true)
	private void hookChunkBuild(float x, float y, float z, ChunkBuilder.ChunkData chunkData, BlockBufferBuilderStorage buffers, CallbackInfoReturnable<Set<BlockEntity>> ci) {
		final Set<BlockEntity> blockEntities = Sets.newHashSet();
		final BlockPos origin = field_20839.getOrigin();
		final ProtoRenderRegion region = protoRegion;
		final AccessChunkRendererData chunkDataAccess = (AccessChunkRendererData) chunkData;
		protoRegion = null;

		if (region != null) {
			for(final BlockEntity blockEntity : region.blockEntities) {
				addBlockEntity(chunkData, blockEntities, blockEntity);
			}

			ChunkRebuildinator.rebuildChunk(x, y, z, chunkDataAccess, buffers, region, origin);

			region.release();
		} else {
			chunkDataAccess.canvas_setOcclusionGraph(OcclusionRegion.ALL_OPEN);
		}

		ci.setReturnValue(blockEntities);
	}

	@Override
	public void canvas_setRegion(ProtoRenderRegion region) {
		protoRegion = region;
	}
}
