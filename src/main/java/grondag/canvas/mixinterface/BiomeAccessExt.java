package grondag.canvas.mixinterface;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess.Storage;

public interface BiomeAccessExt {
	Biome getBiome(int x, int y, int z, Storage storage);
}
