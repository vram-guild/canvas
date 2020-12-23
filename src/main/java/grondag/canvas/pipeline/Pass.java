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
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.pipeline.PipelineConfig.AttachmentConfig;
import grondag.canvas.pipeline.PipelineConfig.PassConfig;
import grondag.canvas.pipeline.PipelineConfig.SamplerConfig;
import grondag.canvas.shader.ProcessShader;

public class Pass {
	private final PassConfig config;
	int fboGlId = -1;
	final int[] binds;
	ProcessShader shader;
	float r, g, b, a;

	Pass(PassConfig config) {
		this.config = config;
		binds = new int[config.samplers.length];
		// WIP: move this to framebuffer config
		//		a = ((config.clearColor >> 24) & 0xFF) / 255f;
		//		r = ((config.clearColor >> 16) & 0xFF) / 255f;
		//		g = ((config.clearColor >> 8) & 0xFF) / 255f;
		//		b = (config.clearColor & 0xFF) / 255f;
	}

	void open(int width, int height) {
		fboGlId = GlStateManager.genFramebuffers();

		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);

		for (int i = 0; i < config.attachments.length; ++i) {
			final AttachmentConfig ac = config.attachments[i];
			final Image img = Pipeline.getImage(ac.image);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + ac.attachmentIndex, GL21.GL_TEXTURE_2D, img.glId(), ac.lod);
		}

		for (int i = 0; i < config.samplers.length; ++i) {
			final SamplerConfig sc = config.samplers[i];
			binds[i] = sc.gameTexture
				? MinecraftClient.getInstance().getTextureManager().getTexture(sc.texture).getGlId()
				: Pipeline.getImage(sc.texture).glId();
		}

		shader = Pipeline.getShader(config.shader);
	}

	void clearIfNeeded() {
		// WIP: implement as built-in program
		//		if (config.clear) {
		//			GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);
		//			GlStateManager.clearColor(r, g, b, a);
		//			GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		//		}
	}

	void activate(int width, int height) {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);

		final int lod = config.lod;

		if (lod > 0) {
			width >>= lod;
			height >>= lod;
		}

		PipelineManager.setProjection(width, height);
		RenderSystem.viewport(0, 0, width, height);

		final int slimit = binds.length;

		for (int i = 0; i < slimit; ++i) {
			GlStateManager.activeTexture(GL21.GL_TEXTURE0 + i);
			GlStateManager.bindTexture(binds[i]);
		}

		shader.activate().lod(config.lod).size(width, height);
	}

	void close() {
		if (fboGlId != -1) {
			GlStateManager.deleteFramebuffers(fboGlId);
			fboGlId = -1;
		}
	}
}
