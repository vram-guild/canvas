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
import grondag.canvas.terrain.util.TerrainExecutor;

public class RenderRegionBuilder {
	private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
	// for use by render thread rebuilds
	TerrainRenderContext mainThreadContext = new TerrainRenderContext();

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
		TerrainExecutor.INSTANCE.clear();
		mainThreadContext.close();
		mainThreadContext = new TerrainRenderContext();
	}

	public void scheduleUpload(Runnable task) {
		uploadQueue.offer(task);
	}

	public boolean isEmpty() {
		return TerrainExecutor.INSTANCE.isEmpty() && uploadQueue.isEmpty();
	}
}
