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

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.Configurator;
import grondag.canvas.mixinterface.ParticleManagerExt;
import grondag.canvas.pipeline.CanvasFrameBufferHacks;
import grondag.canvas.wip.encoding.WipVertexCollectorImpl;
import grondag.canvas.wip.state.WipRenderState;
import grondag.canvas.wip.state.WipVertexState;
import grondag.canvas.wip.state.property.WipTransparency;
import grondag.canvas.wip.state.property.WipWriteMask;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

public enum CanvasParticleRenderer {
	INSTANCE;

	private final WipVertexCollectorImpl collector = new WipVertexCollectorImpl();

	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	private ParticleManagerExt ext;
	private Runnable drawHandler = Runnables.doNothing();

	public void renderParticles(ParticleManager pm, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta) {
		lightmapTextureManager.enable();
		RenderSystem.enableAlphaTest();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.enableFog();
		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(matrixStack.peek().getModel());

		tessellator = Tessellator.getInstance();
		bufferBuilder = tessellator.getBuffer();
		ext = (ParticleManagerExt) pm;
		final Iterator<ParticleTextureSheet> sheets = ext.canvas_textureSheets().iterator();

		while(sheets.hasNext()) {
			final ParticleTextureSheet particleTextureSheet = sheets.next();
			final Iterable<Particle> iterable = ext.canvas_particles().get(particleTextureSheet);

			if (iterable == null) {
				continue;
			}

			final Iterator<Particle> particles = iterable.iterator();
			final VertexConsumer consumer = beginSheet(particleTextureSheet);

			while(particles.hasNext()) {
				final Particle particle = particles.next();

				try {
					particle.buildGeometry(consumer, camera, tickDelta);
				} catch (final Throwable exception) {
					final CrashReport crashReport = CrashReport.create(exception, "Rendering Particle");
					final CrashReportSection crashReportSection = crashReport.addElement("Particle being rendered");
					crashReportSection.add("Particle", particle::toString);
					crashReportSection.add("Particle Type", particleTextureSheet::toString);
					throw new CrashException(crashReport);
				}
			}

			drawHandler.run();
		}

		RenderSystem.popMatrix();
		RenderSystem.depthMask(true);
		RenderSystem.depthFunc(515);
		RenderSystem.disableBlend();
		RenderSystem.defaultAlphaFunc();
		lightmapTextureManager.disable();
		RenderSystem.disableFog();
	}

	private VertexConsumer beginSheet(ParticleTextureSheet particleTextureSheet) {
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		if (Configurator.enableExperimentalPipeline) {
			if (particleTextureSheet == ParticleTextureSheet.TERRAIN_SHEET) {
				if (Configurator.enableBloom) CanvasFrameBufferHacks.startEmissiveCapture(false);
				collector.prepare(RENDER_STATE_TERRAIN);

				drawHandler = () -> {
					RENDER_STATE_TERRAIN.draw(collector);
					if (Configurator.enableBloom) CanvasFrameBufferHacks.endEmissiveCapture();
				};

				return collector;
			} else if (particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_LIT || particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE) {
				if (Configurator.enableBloom) CanvasFrameBufferHacks.startEmissiveCapture(false);
				collector.prepare(RENDER_STATE_OPAQUE_OR_LIT);

				drawHandler = () -> {
					RENDER_STATE_OPAQUE_OR_LIT.draw(collector);
					if (Configurator.enableBloom) CanvasFrameBufferHacks.endEmissiveCapture();
				};

				return collector;
			} else if (particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
				if (Configurator.enableBloom) CanvasFrameBufferHacks.startEmissiveCapture(false);
				collector.prepare(RENDER_STATE_TRANSLUCENT);
				collector.vertexState(VERTEX_STATE_TRANSLUCENT);

				drawHandler = () -> {
					RENDER_STATE_TRANSLUCENT.draw(collector);
					if (Configurator.enableBloom) CanvasFrameBufferHacks.endEmissiveCapture();
				};

				return collector;
			}
		}

		particleTextureSheet.begin(bufferBuilder, ext.canvas_textureManager());
		drawHandler = () -> particleTextureSheet.draw(tessellator);
		return bufferBuilder;
	}

	private static final WipRenderState RENDER_STATE_TERRAIN = WipRenderState.finder()
	.writeMask(WipWriteMask.COLOR_DEPTH)
	.transparency(WipTransparency.DEFAULT)
	.hasColor(true)
	.hasLightmap(true)
	.texture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
	.hasNormal(false).find();

	// MC has two but they are functionally identical
	private static final WipRenderState RENDER_STATE_OPAQUE_OR_LIT = WipRenderState.finder()
	.writeMask(WipWriteMask.COLOR_DEPTH)
	.transparency(WipTransparency.NONE)
	.hasColor(true)
	.hasLightmap(true)
	.texture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
	.hasNormal(false).find();

	private static final WipRenderState RENDER_STATE_TRANSLUCENT = WipRenderState.finder()
	.writeMask(WipWriteMask.COLOR_DEPTH)
	.transparency(WipTransparency.TRANSLUCENT)
	.hasColor(true)
	.hasLightmap(true)
	.texture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
	.hasNormal(false).find();

	private static final int VERTEX_STATE_TRANSLUCENT = WipVertexState.finder().cutout(true).cutout10(true).find();
}
