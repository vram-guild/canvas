/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.render;

import java.util.Iterator;

import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.mixinterface.ParticleManagerExt;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

public abstract class CanvasParticleRenderer {
	private CanvasParticleRenderer() {}

	public static void renderParticles(ParticleManager pm, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f) {
		final ParticleManagerExt ext = (ParticleManagerExt) pm;

		lightmapTextureManager.enable();
		RenderSystem.enableAlphaTest();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.enableFog();
		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(matrixStack.peek().getModel());
		final Iterator<ParticleTextureSheet> var6 = ext.canvas_textureSheets().iterator();

		while(true) {
			ParticleTextureSheet particleTextureSheet;
			Iterable<Particle> iterable;

			do {
				if (!var6.hasNext()) {
					RenderSystem.popMatrix();
					RenderSystem.depthMask(true);
					RenderSystem.depthFunc(515);
					RenderSystem.disableBlend();
					RenderSystem.defaultAlphaFunc();
					lightmapTextureManager.disable();
					RenderSystem.disableFog();
					return;
				}

				particleTextureSheet = var6.next();
				iterable = ext.canvas_particles().get(particleTextureSheet);
			} while(iterable == null);

			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			final Tessellator tessellator = Tessellator.getInstance();
			final BufferBuilder bufferBuilder = tessellator.getBuffer();
			particleTextureSheet.begin(bufferBuilder, ext.canvas_textureManager());
			final Iterator<Particle> var11 = iterable.iterator();

			while(var11.hasNext()) {
				final Particle particle = var11.next();

				try {
					particle.buildGeometry(bufferBuilder, camera, f);
				} catch (final Throwable var16) {
					final CrashReport crashReport = CrashReport.create(var16, "Rendering Particle");
					final CrashReportSection crashReportSection = crashReport.addElement("Particle being rendered");
					crashReportSection.add("Particle", particle::toString);
					crashReportSection.add("Particle Type", particleTextureSheet::toString);
					throw new CrashException(crashReport);
				}
			}

			particleTextureSheet.draw(tessellator);
		}
	}
}
