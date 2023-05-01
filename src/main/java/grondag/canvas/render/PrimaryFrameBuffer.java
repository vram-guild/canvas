/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.render;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.PipelineManager;

public class PrimaryFrameBuffer extends MainTarget {
	public PrimaryFrameBuffer(int width, int height) {
		super(width, height);
	}

	@Override
	public void destroyBuffers() {
		RenderSystem.assertOnGameThreadOrInit();
		unbindRead();
		unbindWrite();

		//NB: pipeline manager handles close
	}

	@Override
	protected void createFrameBuffer(int width, int height) {
		this.width = width;
		this.viewWidth = width;
		this.height = height;
		this.viewHeight = height;
		PipelineManager.init(this, width, height);
	}

	@Override
	public void createBuffers(int width, int height, boolean getError) {
		RenderSystem.assertOnGameThreadOrInit();
		viewWidth = width;
		viewHeight = height;
		this.width = width;
		this.height = height;

		// UGLY - throwing away what seems to be a spurious INVALID_VALUE error here
		GlStateManager._getError();

		PipelineManager.onResize(this, width, height);

		checkStatus();
		unbindRead();
	}

	private int clearCount = 0;

	@Override
	public void clear(boolean getError) {
		// Should be handled in pipeline buffers so do nothing
		// and warn when it does.

		// We ignore the first call because it happens before we can prevent it
		// and this avoids people asking us why the message is in the log.
		if (++clearCount == 2) {
			CanvasMod.LOG.info("Another mod is clearing the vanilla framebuffer. This message is a diagnostic aid and does not necessarily indicate a problem.");
		}
	}
}
