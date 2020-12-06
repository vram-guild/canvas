/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.terrain.region;

import java.util.Queue;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.terrain.util.ChunkRenderExecutor;

public class RenderRegionBuilder {
	public final ChunkRenderExecutor executor = new ChunkRenderExecutor();
	private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
	// for use by render thread rebuilds
	TerrainRenderContext mainThreadContext = new TerrainRenderContext();

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
		mainThreadContext.close();
		mainThreadContext = new TerrainRenderContext();
	}

	public void scheduleUpload(Runnable task) {
		uploadQueue.offer(task);
	}

	public boolean isEmpty() {
		return executor.isEmpty() && uploadQueue.isEmpty();
	}
}
