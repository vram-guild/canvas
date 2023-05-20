/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.terrain.util;

import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import grondag.canvas.mixinterface.LevelChunkExt;

//FEAT: per-vertex blending (quality)
public class ChunkColorCache implements BiomeManager.NoiseBiomeSource {
	private static final Minecraft mc = Minecraft.getInstance();
	private static int VERSION = 0;
	private final LevelChunk chunk;
	private final ClientLevel world;
	private final int chunkX;
	private final int chunkZ;
	private final int version;
	private final BiomeColorCache grassCache = new BiomeColorCache(BiomeColors.GRASS_COLOR_RESOLVER, c -> c.grassCache);
	private final BiomeColorCache foliageCache = new BiomeColorCache(BiomeColors.FOLIAGE_COLOR_RESOLVER, c -> c.foliageCache);
	private final BiomeColorCache waterCache = new BiomeColorCache(BiomeColors.WATER_COLOR_RESOLVER, c -> c.waterCache);
	private final MutableBlockPos searchPos = new MutableBlockPos();

	public ChunkColorCache(ClientLevel world, LevelChunk chunk) {
		this.world = world;
		this.chunk = chunk;
		version = VERSION;
		final ChunkPos pos = chunk.getPos();
		chunkX = pos.x;
		chunkZ = pos.z;
	}

	public static ChunkColorCache get(LevelChunk chunk) {
		return ((LevelChunkExt) chunk).canvas_colorCache();
	}

	public static void invalidate() {
		VERSION++;
	}

	public boolean isInvalid() {
		return version < VERSION;
	}

	public Biome getBiome(int x, int y, int z) {
		return world.getBiomeManager().getBiome(searchPos.set(x, y, z)).value();
	}

	public Holder<Biome> getBiomeHolder(int x, int y, int z) {
		return world.getBiomeManager().getBiome(searchPos.set(x, y, z));
	}

	private LevelChunk getChunk(int cx, int cz) {
		if (cx == chunkX && cz == chunkZ) {
			return chunk;
		} else {
			return world.getChunk(cx, cz);
		}
	}

	@Override
	public Holder<Biome> getNoiseBiome(int x, int y, int z) {
		final ChunkAccess chunk = getChunk(x >> 2, z >> 2);

		if (chunk != null) {
			return chunk.getNoiseBiome(x, y, z);
		}

		return world.getUncachedNoiseBiome(x, y, z);
	}

	public int getColor(int x, int y, int z, ColorResolver colorResolver) {
		if (colorResolver == BiomeColors.GRASS_COLOR_RESOLVER) {
			return grassCache.getColor(x, y, z);
		} else if (colorResolver == BiomeColors.FOLIAGE_COLOR_RESOLVER) {
			return foliageCache.getColor(x, y, z);
		} else if (colorResolver == BiomeColors.WATER_COLOR_RESOLVER) {
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
			final int radius = mc.options.biomeBlendRadius().get();

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
