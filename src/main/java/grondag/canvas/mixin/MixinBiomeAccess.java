package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeAccess.Storage;
import net.minecraft.world.biome.source.BiomeAccessType;

import grondag.canvas.mixinterface.BiomeAccessExt;

@Mixin(BiomeAccess.class)
public class MixinBiomeAccess implements BiomeAccessExt {
	@Shadow private long seed;
	@Shadow private BiomeAccessType type;

	@Override
	public Biome getBiome(int x, int y, int z, Storage storage) {
		return type.getBiome(seed, x, y, z, storage);
	}
}
