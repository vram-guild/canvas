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

package grondag.canvas.terrain;

import grondag.canvas.mixinterface.BiomeAccessExt;
import grondag.canvas.mixinterface.WorldChunkExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.level.ColorResolver;

import java.util.function.Function;

//TODO: per-vertex blending (quality)
@Environment(value = EnvType.CLIENT)
public class ChunkColorCache implements BiomeAccess.Storage {
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private static int VERSION = 0;
	private final WorldChunk chunk;
	private final ClientWorld world;
	private final int chunkX;
	private final int chunkZ;
	private final int version;
	private final BiomeColorCache grassCache = new BiomeColorCache(BiomeColors.GRASS_COLOR, c -> c.grassCache);
	private final BiomeColorCache foliageCache = new BiomeColorCache(BiomeColors.FOLIAGE_COLOR, c -> c.foliageCache);
	private final BiomeColorCache waterCache = new BiomeColorCache(BiomeColors.WATER_COLOR, c -> c.waterCache);

	public ChunkColorCache(ClientWorld world, WorldChunk chunk) {
		this.world = world;
		this.chunk = chunk;
		version = VERSION;
		final ChunkPos pos = chunk.getPos();
		chunkX = pos.x;
		chunkZ = pos.z;
	}

	public static ChunkColorCache get(WorldChunk chunk) {
		return ((WorldChunkExt) chunk).canvas_colorCache();
	}

	public static void invalidate() {
		VERSION++;
	}

	public boolean isInvalid() {
		return version < VERSION;
	}

	private Biome getBiome(int x, int y, int z) {
		return ((BiomeAccessExt) world.getBiomeAccess()).getBiome(x, y, z, this);
	}

	private WorldChunk getChunk(int cx, int cz) {
		if (cx == chunkX && cz == chunkZ) {
			return chunk;
		} else {
			return world.getChunk(cx, cz);
		}
	}

	@Override
	public Biome getBiomeForNoiseGen(int x, int y, int z) {
		final Chunk chunk = getChunk(x >> 2, z >> 2);

		if (chunk != null) {
			final BiomeArray ba = chunk.getBiomeArray();

			if (ba != null) {
				return chunk.getBiomeArray().getBiomeForNoiseGen(x, y, z);
			}
		}

		return world.getGeneratorStoredBiome(x, y, z);
	}

	public int getColor(int x, int y, int z, ColorResolver colorResolver) {
		if (colorResolver == BiomeColors.GRASS_COLOR) {
			return grassCache.getColor(x, y, z);
		} else if (colorResolver == BiomeColors.FOLIAGE_COLOR) {
			return foliageCache.getColor(x, y, z);
		} else if (colorResolver == BiomeColors.WATER_COLOR) {
			return waterCache.getColor(x, y, z);
		} else {
			return -1;
		}
	}

	private class BiomeColorCache {
		private static final int BASE_INDEX = 0;
		private static final int BASE_CONTROL = BASE_INDEX + 256;
		private static final int BLENDED_INDEX = BASE_CONTROL + 8;
		private static final int BLENDED_CONTROL = BLENDED_INDEX + 256;
		private final ColorResolver colorResolver;
		private final Function<ChunkColorCache, BiomeColorCache> cacheFunc;
		private final int[] data = new int[512 + 16];

		private BiomeColorCache(ColorResolver colorResolver, Function<ChunkColorCache, BiomeColorCache> cacheFunc) {
			this.colorResolver = colorResolver;
			this.cacheFunc = cacheFunc;
		}

		private int getBaseColor(int x, int y, int z) {
			final int cx = x >> 4;
			final int cz = z >> 4;

			if (cx == chunkX && cz == chunkZ) {
				return getLocalBaseColor(x, y, z);
			} else {
				return cacheFunc.apply(get(world.getChunk(cx, cz))).getLocalBaseColor(x, y, z);
			}
		}

		private int getLocalBaseColor(int x, int y, int z) {
			final int index = (x & 0xF) | ((z & 0xF) << 4);
			final int controlIndex = BASE_CONTROL + (index >> 5);
			final int controlMask = 1 << (index & 31);

			if ((data[controlIndex] & controlMask) == 0) {
				final int result = computeLocalBaseColor(x, y, z);
				data[controlIndex] |= controlMask;
				data[index] = result;
				return result;
			} else {
				return data[index];
			}
		}

		private int computeLocalBaseColor(int x, int y, int z) {
			return colorResolver.getColor(getBiome(x, y, z), x, z);
		}

		private int getColor(int x, int y, int z) {
			final int cx = x >> 4;
			final int cz = z >> 4;

			if (cx == chunkX && cz == chunkZ) {
				return getLocalBlendedColor(x, y, z);
			} else {
				return cacheFunc.apply(get(world.getChunk(cx, cz))).getLocalBlendedColor(x, y, z);
			}
		}

		private int getLocalBlendedColor(int x, int y, int z) {
			final int index = (x & 0xF) | ((z & 0xF) << 4);
			final int controlIndex = BLENDED_CONTROL + (index >> 5);
			final int controlMask = 1 << (index & 31);

			if ((data[controlIndex] & controlMask) == 0) {
				final int result = computeLocalBlendedColor(x, y, z);
				data[controlIndex] |= controlMask;
				data[index + BLENDED_INDEX] = result;
				return result;
			} else {
				return data[index + BLENDED_INDEX];
			}
		}

		private int computeLocalBlendedColor(int xIn, int yIn, int zIn) {
			final int radius = mc.options.biomeBlendRadius;

			if (radius == 0) {
				return getLocalBaseColor(xIn, yIn, zIn);
			} else {
				final int sampleCount = (radius * 2 + 1) * (radius * 2 + 1);
				int r = 0;
				int g = 0;
				int b = 0;

				final int xMax = xIn + radius;
				final int zMax = zIn + radius;

				for (int x = xIn - radius; x <= xMax; x++) {
					for (int z = zIn - radius; z <= zMax; z++) {
						final int color = getBaseColor(x, yIn, z);
						g += (color >> 8) & 255;
						r += (color >> 16) & 255;
						b += color & 255;
					}
				}

				return (r / sampleCount & 255) << 16 | (g / sampleCount & 255) << 8 | b / sampleCount & 255;
			}
		}
	}
}
