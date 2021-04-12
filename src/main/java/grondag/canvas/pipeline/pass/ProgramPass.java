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
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL46;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

import grondag.canvas.pipeline.Image;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.shader.ProcessShader;

import java.io.IOException;

class ProgramPass extends Pass {
	final int[] binds;
	final int[] bindTargets;

	ProcessShader shader;

	ProgramPass(PassConfig config) {
		super(config);

		shader = Pipeline.getShader(config.program.name);

		binds = new int[config.samplerImages.length];
		bindTargets = new int[config.samplerImages.length];

		for (int i = 0; i < config.samplerImages.length; ++i) {
			final String imageName = config.samplerImages[i].name;

			int imageBind = 0;
			int bindTarget = GL46.GL_TEXTURE_2D;

			if (imageName.contains(":")) {
				final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
				final Identifier identifier = new Identifier(imageName);
				final AbstractTexture tex = textureManager.getTexture(identifier);

				AbstractTexture usedTex;
				if (tex == null) {
					ResourceTexture resourceTexture = new ResourceTexture(identifier);
					try {
						resourceTexture.load(MinecraftClient.getInstance().getResourceManager());
						textureManager.registerTexture(identifier, resourceTexture);
						usedTex = textureManager.getTexture(identifier);
					} catch (IOException e) {
						usedTex = null;
					}
				} else {
					usedTex = tex;
				}

				if (usedTex != null) {
					imageBind = usedTex.getGlId();
				}
			} else {
				final Image img = Pipeline.getImage(imageName);

				if (img != null) {
					imageBind = img.glId();
					bindTarget = img.config.target;
				}
			}

			binds[i] = imageBind;
			bindTargets[i] = bindTarget;
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
			CanvasTextureState.activeTextureUnit(GL21.GL_TEXTURE0 + i);
			CanvasTextureState.bindTexture(bindTargets[i], binds[i]);
		}

		shader.activate().lod(config.lod).size(width, height);

		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
	}

	@Override
	public void close() {
		// NOOP
	}
}
