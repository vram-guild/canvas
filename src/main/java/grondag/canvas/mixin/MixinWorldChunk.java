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

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import grondag.canvas.mixinterface.WorldChunkExt;
import grondag.canvas.terrain.util.ChunkColorCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(LevelChunk.class)
public class MixinWorldChunk implements WorldChunkExt {
	private @Nullable ChunkColorCache colorCache;
	@Shadow private Level world;

	@Override
	public ChunkColorCache canvas_colorCache() {
		ChunkColorCache result = colorCache;

		if (result == null || result.isInvalid()) {
			result = new ChunkColorCache((ClientLevel) world, (LevelChunk) (Object) this);
			colorCache = result;
		}

		return result;
	}

	@Override
	public void canvas_clearColorCache() {
		colorCache = null;
	}
}
