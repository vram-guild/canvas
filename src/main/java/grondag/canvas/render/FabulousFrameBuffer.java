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
