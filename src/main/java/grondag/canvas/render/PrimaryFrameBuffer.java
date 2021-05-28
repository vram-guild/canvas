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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.WindowFramebuffer;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.PipelineManager;

public class PrimaryFrameBuffer extends WindowFramebuffer {
	public PrimaryFrameBuffer(int width, int height) {
		super(width, height);
	}

	@Override
	public void delete() {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		endRead();
		endWrite();

		//NB: pipeline manager handles close
	}

	@Override
	public void initFbo(int width, int height, boolean getError) {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		viewportWidth = width;
		viewportHeight = height;
		textureWidth = width;
		textureHeight = height;

		// UGLY - throwing away what seems to be a spurious INVALID_VALUE error here
		GlStateManager._getError();

		PipelineManager.init(this, width, height);

		checkFramebufferStatus();
		endRead();
	}

	@Override
	public void clear(boolean getError) {
		// NOOP - should be done in pipeline buffers
		CanvasMod.LOG.info("Unmanaged framebuffer clear");
	}
}
