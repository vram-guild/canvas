package grondag.canvas.terrain;

import java.util.Queue;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;

public class RenderRegionBuilder {
	private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
	public final ChunkRenderExecutor executor = new ChunkRenderExecutor();

	// for use by render thread rebuilds
	final TerrainRenderContext mainThreadContext = new TerrainRenderContext();

	public RenderRegionBuilder() {
		// PERF: limit vertex collectors similarly, somehow
		//		final int memoryLimitedBufferCount = Math.max(1, (int)(Runtime.getRuntime().maxMemory() * 0.3D) / (RenderLayer.getBlockLayers().stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() * 4) - 1);
		//		final int processorCount = Runtime.getRuntime().availableProcessors();
		//		final int architectureMaxBuilderCount = is64Bit ? processorCount : Math.min(processorCount, 4);
		//		final int l = Math.max(1, Math.max(architectureMaxBuilderCount, memoryLimitedBufferCount));

	}

	public String getDebugString() {
		return String.format("not available");
	}

	public boolean upload() {
		assert RenderSystem.isOnRenderThread();

		Runnable task = uploadQueue.poll();

		final boolean didRun = task != null;

		while (task != null) {
			task.run();
			task = uploadQueue.poll();
		}

		return didRun;
	}

	public void reset() {
		executor.clear();
	}

	public void scheduleUpload(Runnable task) {
		uploadQueue.offer(task);
	}

	public boolean isEmpty() {
		return executor.isEmpty() && uploadQueue.isEmpty();
	}
}

