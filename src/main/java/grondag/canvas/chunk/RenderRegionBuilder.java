package grondag.canvas.chunk;

import java.util.Queue;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

public class RenderRegionBuilder {
	private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
	public final ChunkRenderExecutor executor = new ChunkRenderExecutor();
	public final WorldRenderer worldRenderer;
	ClientWorld world;
	private Vec3d cameraPosition;

	// for use by render thread rebuilds
	final TerrainRenderContext mainThreadContext = new TerrainRenderContext();

	public RenderRegionBuilder(ClientWorld world, WorldRenderer worldRenderer, boolean is64Bit) {
		cameraPosition = Vec3d.ZERO;
		this.world = world;
		this.worldRenderer = worldRenderer;

		// TODO: limit vertex collectors similarly, somehow
		//		final int memoryLimitedBufferCount = Math.max(1, (int)(Runtime.getRuntime().maxMemory() * 0.3D) / (RenderLayer.getBlockLayers().stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() * 4) - 1);
		//		final int processorCount = Runtime.getRuntime().availableProcessors();
		//		final int architectureMaxBuilderCount = is64Bit ? processorCount : Math.min(processorCount, 4);
		//		final int l = Math.max(1, Math.max(architectureMaxBuilderCount, memoryLimitedBufferCount));

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

