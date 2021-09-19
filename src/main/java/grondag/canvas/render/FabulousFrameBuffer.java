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

package grondag.canvas.render;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.pipeline.PipelineManager;

public class FabulousFrameBuffer extends MainTarget {
	public FabulousFrameBuffer(int fboId, int colorId, int depthId) {
		super(PipelineManager.width(), PipelineManager.height());
		setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

		frameBufferId = fboId;
		colorTextureId = colorId;
		depthBufferId = depthId;

		checkStatus();
		unbindRead();
	}

	@Override
	public void destroyBuffers() {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		unbindRead();
		unbindWrite();

		// nothing to do here - pipeline will clean up
	}

	@Override
	public void createBuffers(int width, int height, boolean getError) {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		viewWidth = width;
		viewHeight = height;
		this.width = width;
		this.height = height;

		// rest is handled in init that accepts IDs from pipeline
	}

	@Override
	public void clear(boolean getError) {
		// NOOP - should be done in pipeline buffers
		// removed because vanilla clouds does it...
		//assert false : "Unmanaged framebuffer clear";
	}
}
