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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.Direction;

import grondag.canvas.mixinterface.ChunkInfoExt;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer$ChunkInfo")
public class MixinChunkInfo implements ChunkInfoExt {

	@Shadow private ChunkBuilder.BuiltChunk chunk;
	@Shadow private Direction direction;
	@Shadow private byte cullingState;
	@Shadow private int propagationLevel;

	@Override
	public Direction canvas_entryFace() {
		return direction;
	}

	@Override
	public BuiltChunk canvas_chunk() {
		return chunk;
	}

	@Override
	public int canvas_propagationLevel() {
		return propagationLevel;
	}

	@Override
	public byte canvas_backtrackState() {
		return cullingState;
	}

	@Override
	public boolean canvas_isBacktrack(Direction direction) {
		return (cullingState & 1 << direction.ordinal()) > 0;
	}

	@Override
	public void canvas_updateBacktrackState(byte b, Direction direction) {
		cullingState = (byte)(cullingState | b | 1 << direction.ordinal());
	}
}
