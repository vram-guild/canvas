package grondag.canvas.chunk;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;

public class RenderRegionBuilder {
	private static final Logger LOGGER = LogManager.getLogger();
	final ArrayBlockingQueue<BlockBufferBuilderStorage> workerBufferStorage;
	private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
	public final ChunkRenderExecutor executor = new ChunkRenderExecutor();
	public final WorldRenderer worldRenderer;
	ClientWorld world;
	private Vec3d cameraPosition;

	// for use by render thread rebuilds
	final BlockBufferBuilderStorage mainThreadBufferStorage;
	final TerrainRenderContext mainThreadContext = new TerrainRenderContext();

	public RenderRegionBuilder(ClientWorld world, WorldRenderer worldRenderer, boolean is64Bit, BlockBufferBuilderStorage blockBufferBuilderStorage) {
		cameraPosition = Vec3d.ZERO;
		this.world = world;
		this.worldRenderer = worldRenderer;
		final int memoryLimitedBufferCount = Math.max(1, (int)(Runtime.getRuntime().maxMemory() * 0.3D) / (RenderLayer.getBlockLayers().stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() * 4) - 1);
		final int processorCount = Runtime.getRuntime().availableProcessors();
		final int architectureMaxBuilderCount = is64Bit ? processorCount : Math.min(processorCount, 4);
		final int l = Math.max(1, Math.max(architectureMaxBuilderCount, memoryLimitedBufferCount));
		// TODO: replace buffer structure
		//		final int l = Math.max(1, Math.min(architectureMaxBuilderCount, memoryLimitedBufferCount));
		mainThreadBufferStorage = blockBufferBuilderStorage;
		final ArrayList<BlockBufferBuilderStorage> list = Lists.newArrayListWithExpectedSize(l);

		try {
			for(int m = 0; m < l; ++m) {
				list.add(new BlockBufferBuilderStorage());
			}
		} catch (final OutOfMemoryError var14) {
			LOGGER.warn("Allocated only {}/{} buffers", list.size(), l);
			final int n = Math.min(list.size() * 2 / 3, list.size() - 1);

			for(int o = 0; o < n; ++o) {
				list.remove(list.size() - 1);
			}

			System.gc();
		}

		workerBufferStorage = new ArrayBlockingQueue<>(list.size());
		list.forEach(b -> workerBufferStorage.add(b));
	}

	public void setWorld(ClientWorld world) {
		this.world = world;
	}

	public String getDebugString() {
		return String.format("not available");
	}

	public void setCameraPosition(Vec3d vec3d) {
		cameraPosition = vec3d;
	}

	public Vec3d getCameraPosition() {
		return cameraPosition;
	}

	public boolean upload() {
		Runnable task = uploadQueue.poll();

		final boolean didRun = task != null;

		while (task != null) {
			task.run();
			task = uploadQueue.poll();
		}

		return didRun;
	}

	public void reset() {
		clear();
	}

	public void scheduleUpload(Runnable task) {
		uploadQueue.offer(task);
	}


	private void clear() {
		executor.clear();
	}

	public boolean isEmpty() {
		return executor.isEmpty() && uploadQueue.isEmpty();
	}

	public void stop() {
		clear();
		workerBufferStorage.clear();
	}
}

