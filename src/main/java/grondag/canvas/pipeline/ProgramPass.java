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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.shader.ProcessShader;

class ProgramPass extends Pass {
	final int[] binds;
	ProcessShader shader;

	ProgramPass(PassConfig config) {
		super(config);
		binds = new int[config.samplerNames.length];

		for (int i = 0; i < config.samplerNames.length; ++i) {
			final String imageName = config.samplerNames[i];

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

		shader = Pipeline.getShader(config.programName);

		if (shader == null) {
			CanvasMod.LOG.warn(String.format("Unable to find shader %s for pass %s.  Pass will be skipped.", config.programName, config.name));
			isValid = false;
		} else if (shader.samplerCount() != binds.length) {
			CanvasMod.LOG.warn(String.format("Shader %s in pass %s expects %d samplers but the pass binds %d.  Pass may not operate correctly.",
					config.programName, config.name, shader.samplerCount(), binds.length));
		}
	}

	@Override
	void run(int width, int height) {
		if (fbo == null) {
			return;
		}

		fbo.bind(width, height);

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
	void close() {
		// NOOP
	}
}