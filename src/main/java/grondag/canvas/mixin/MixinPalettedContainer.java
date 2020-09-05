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
	@Shadow private T defaultValue;
	@Shadow protected PackedIntegerArray data;
	@Shadow private Palette<T> palette;

	@SuppressWarnings("unchecked")
	@Override
	public PaletteCopy canvas_paletteCopy() {
		return ChunkPaletteCopier.captureCopy((Palette<BlockState>) palette, data, (BlockState)defaultValue);
	}
}
