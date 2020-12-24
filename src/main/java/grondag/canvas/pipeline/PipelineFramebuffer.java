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

package grondag.canvas.pipeline;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.pipeline.config.AttachmentConfig;
import grondag.canvas.pipeline.config.FramebufferConfig;

class PipelineFramebuffer {
	final FramebufferConfig config;
	int fboGlId = -1;
	float r, g, b, a;

	PipelineFramebuffer(FramebufferConfig config, int width, int height) {
		this.config = config;

		// WIP: handle separate colors/masks per attachment
		final int color = config.attachments[0].clearColor;
		a = ((color >> 24) & 0xFF) / 255f;
		r = ((color >> 16) & 0xFF) / 255f;
		g = ((color >> 8) & 0xFF) / 255f;
		b = (color & 0xFF) / 255f;

		open(width, height);
	}

	void open(int width, int height) {
		fboGlId = GlStateManager.genFramebuffers();

		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);

		for (int i = 0; i < config.attachments.length; ++i) {
			final AttachmentConfig ac = config.attachments[i];
			final Image img = Pipeline.getImage(ac.imageName);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + i, GL21.GL_TEXTURE_2D, img.glId(), ac.lod);
		}
	}

	void clear() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);
		GlStateManager.clearColor(r, g, b, a);
		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
	}

	void bind(int width, int height) {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);
	}

	void close() {
		if (fboGlId != -1) {
			GlStateManager.deleteFramebuffers(fboGlId);
			fboGlId = -1;
		}
	}
}
