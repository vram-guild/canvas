package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;

import grondag.canvas.CanvasMod;

public class LightDataManager {
	private static final int REGION_COUNT_LENGTH_WISE = 16;
	// private static final int INITIAL_LIMIT = REGION_COUNT_LENGTH_WISE * REGION_COUNT_LENGTH_WISE * REGION_COUNT_LENGTH_WISE;
	public static final LightDataManager INSTANCE = new LightDataManager();

	private final Long2ObjectMap<LightRegionData> allocated = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
	// initial size based on 8 chunk render distance

	// placeholder
	private int originBlockX = -8 * 16;
	private int originBlockY = 64;
	private int originBlockZ = -8 * 16;

	private int size = REGION_COUNT_LENGTH_WISE;
	private LightDataTexture texture;

	{
		allocated.defaultReturnValue(null);
	}

	// TODO: stuff
	public static void initialize() {

	}

	public void update() {
		if (texture == null) {
			initializeTexture();
		}

		// if (texture.size < supposedTextureSize()) {
		// 	expandTexture();
		// }

		allocated.forEach((key, data) -> {
			if (data.isDirty()) {
				texture.upload(
						data.regionOriginBlockX - originBlockX,
						data.regionOriginBlockY - originBlockY,
						data.regionOriginBlockZ - originBlockZ,
						data.getBuffer());
				data.clearDirty();
			}
		});
	}

	public LightRegionData getOrAllocate(BlockPos regionOrigin) {
		LightRegionData data = allocated.get(regionOrigin.asLong());

		if (data == null) {
			return allocate(regionOrigin);
		}

		return data;
	}

	public boolean withinExtents(BlockPos pos) {
		return withinExtents(pos.getX(), pos.getY(), pos.getZ());
	}

	public boolean withinExtents(int x, int y, int z) {
		return (x >= originBlockX && x < originBlockX + sizeInBlocks())
				&& (y >= originBlockY && y < originBlockY + sizeInBlocks())
				&& (z >= originBlockZ && z < originBlockZ + sizeInBlocks());
	}

	// public boolean isAllocated(BlockPos regionOrigin) {
	// 	return allocated.containsKey(regionOrigin.asLong());
	// }

	private LightRegionData allocate(BlockPos regionOrigin) {
		// if (allocated.size() == size - 1) {
		// 	requestExpand();
		// }

		CanvasMod.LOG.info("allocating for " + regionOrigin);

		final LightRegionData data = new LightRegionData(regionOrigin.getX(), regionOrigin.getY(), regionOrigin.getZ());
		allocated.put(regionOrigin.asLong(), data);

		return data;
	}

	// private void requestExpand() {
	// 	// I'm scared
	// 	final int newLimit = size * 2;
	// 	size = newLimit;
	//
	// 	CanvasMod.LOG.info(String.format("Reallocating light data texture from %d to %d! This is huge!", size, newLimit));
	// }

	private int sizeInBlocks() {
		return size * LightRegionData.Const.WIDTH;
	}

	private void initializeTexture() {
		texture = new LightDataTexture(sizeInBlocks());
	}

	public void close() {
		texture.close();

		for (var data:allocated.values()) {
			data.close();
		}

		allocated.clear();
	}

	public int getTexture(String imageName) {
		if (texture == null) {
			initializeTexture();
		}

		if (imageName.equals("_cv_debug_light_data")) {
			return texture.getTexId();
		}

		return -1;
	}

	// private void expandTexture() {
	// 	if (texture != null && texture.size == supposedTextureSize()) {
	// 		return;
	// 	}
	//
	// 	if (texture != null) {
	// 		texture.close();
	// 		texture = null;
	// 	}
	//
	// 	initializeTexture();
	// }
}
