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

package grondag.canvas.pipeline.pass;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.Image;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.shader.ProcessShader;

class ProgramPass extends Pass {
	final int[] binds;
	ProcessShader shader;

	ProgramPass(PassConfig config) {
		super(config);
		binds = new int[config.samplerImages.length];

		for (int i = 0; i < config.samplerImages.length; ++i) {
			final String imageName = config.samplerImages[i].name;

			int imageBind = 0;

			if (imageName.contains(":")) {
				final AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier(imageName));

				if (tex != null) {
					imageBind = tex.getGlId();
				}
			} else {
				final Image img = Pipeline.getImage(imageName);

				if (img != null) {
					imageBind = img.glId();
				}
			}

			if (imageBind == 0) {
				CanvasMod.LOG.warn(String.format("Unable to find image binding %s for pass %s.  Pass will be skipped.", imageName, config.name));
				isValid = false;
			}

			binds[i] = imageBind;
		}

		// WIP: should be able to remove all this after config validation is complete
		shader = Pipeline.getShader(config.program.name);

		if (shader == null) {
			CanvasMod.LOG.warn(String.format("Unable to find shader %s for pass %s.  Pass will be skipped.", config.program.name, config.name));
			isValid = false;
		} else if (shader.samplerCount() != binds.length) {
			CanvasMod.LOG.warn(String.format("Shader %s in pass %s expects %d samplers but the pass binds %d.  Pass may not operate correctly.",
					config.program.name, config.name, shader.samplerCount(), binds.length));
		}
	}

	@Override
	public void run(int width, int height) {
		if (fbo == null) {
			return;
		}

		fbo.bind();

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

		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
	}

	@Override
	public void close() {
		// NOOP
	}
}
