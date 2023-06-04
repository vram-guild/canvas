package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueues;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

public class LightDataManager {
	private static final int REGION_COUNT_LENGTH_WISE = 16;
	// private static final int INITIAL_LIMIT = REGION_COUNT_LENGTH_WISE * REGION_COUNT_LENGTH_WISE * REGION_COUNT_LENGTH_WISE;
	public static final LightDataManager INSTANCE = new LightDataManager();

	private final Long2ObjectMap<LightRegion> allocated = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
	// initial size based on 8 chunk render distance
	private final LongPriorityQueue updateQueue = LongPriorityQueues.synchronize(new LongArrayFIFOQueue(512));

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

	public void update(BlockAndTintGetter blockView) {
		if (texture == null) {
			initializeTexture();
		}

		// if (texture.size < supposedTextureSize()) {
		// 	expandTexture();
		// }

		synchronized (updateQueue) {
			while (!updateQueue.isEmpty()) {
				final LightRegion lightRegion = allocated.get(updateQueue.dequeueLong());

				if (lightRegion.isClosed()) {
					continue;
				}

				lightRegion.update(blockView);

				if (lightRegion.lightData.isDirty()) {
					texture.upload(
							lightRegion.lightData.regionOriginBlockX - originBlockX,
							lightRegion.lightData.regionOriginBlockY - originBlockY,
							lightRegion.lightData.regionOriginBlockZ - originBlockZ,
							lightRegion.lightData.getBuffer());
					lightRegion.lightData.clearDirty();
				}
			}
		}
	}

	public LightRegion getOrAllocate(BlockPos origin) {
		LightRegion lightRegion = allocated.get(origin.asLong());

		if (lightRegion == null) {
			return allocate(origin);
		}

		return lightRegion;
	}

	public LightRegion getFromBlock(BlockPos blockPos) {
		final long key = BlockPos.asLong(
				blockPos.getX() & ~LightRegionData.Const.WIDTH_MASK,
				blockPos.getY() & ~LightRegionData.Const.WIDTH_MASK,
				blockPos.getZ() & ~LightRegionData.Const.WIDTH_MASK);
		return allocated.get(key);
	}

	public void deallocate(BlockPos regionOrigin) {
		final LightRegion lightRegion = allocated.get(regionOrigin.asLong());

		if (lightRegion != null && lightRegion.lightData != null) {
			lightRegion.lightData.close();
		}

		allocated.remove(regionOrigin.asLong());
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

	private LightRegion allocate(BlockPos origin) {
		// if (allocated.size() == size - 1) {
		// 	requestExpand();
		// }

		final LightRegion lightRegion = new LightRegion(origin);
		allocated.put(origin.asLong(), lightRegion);

		return lightRegion;
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

		synchronized (allocated) {
			for (var lightRegion : allocated.values()) {
				if (lightRegion.lightData != null) {
					lightRegion.lightData.close();
				}
			}

			allocated.clear();
		}
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

	public void queueUpdate(LightRegion lightRegion) {
		updateQueue.enqueue(lightRegion.origin);
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
