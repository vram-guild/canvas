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
package grondag.canvas.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.mixinterface.BuiltChunkStorageExt;

@Mixin(BuiltChunkStorage.class)
public abstract class MixinBuiltChunkStorage implements BuiltChunkStorageExt {
	@Nullable
	@Shadow protected abstract ChunkBuilder.BuiltChunk getRenderedChunk(BlockPos blockPos);

	@Override
	public BuiltChunk canvas_getRendereredChunk(BlockPos pos) {
		return getRenderedChunk(pos);
	}
}
