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

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;

import grondag.canvas.mixinterface.PalettedContainerExt;
import grondag.canvas.terrain.ChunkPaletteCopier;
import grondag.canvas.terrain.ChunkPaletteCopier.PaletteCopy;

@Mixin(PalettedContainer.class)
public abstract class MixinPalettedContainer<T> implements PalettedContainerExt {
	@Shadow private T field_12935;
	@Shadow protected PackedIntegerArray data;
	@Shadow private Palette<T> palette;

	@SuppressWarnings("unchecked")
	@Override
	public PaletteCopy canvas_paletteCopy() {
		return ChunkPaletteCopier.captureCopy((Palette<BlockState>) palette, data, (BlockState)field_12935);
	}
}
