/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.terrain.region;

import java.util.Queue;

import com.google.common.collect.Queues;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.apiimpl.rendercontext.CanvasTerrainRenderContext;
import grondag.canvas.terrain.util.TerrainExecutor;

public class RenderRegionBuilder {
	private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
	// for use by render thread rebuilds
	CanvasTerrainRenderContext mainThreadContext = new CanvasTerrainRenderContext();

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
		mainThreadContext = new CanvasTerrainRenderContext();
	}

	public void scheduleUpload(Runnable task) {
		uploadQueue.offer(task);
	}

	public boolean isEmpty() {
		return TerrainExecutor.INSTANCE.isEmpty() && uploadQueue.isEmpty();
	}
}
