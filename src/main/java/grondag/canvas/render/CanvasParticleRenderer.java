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
import grondag.canvas.mixinterface.ParticleExt;
import grondag.canvas.mixinterface.ParticleManagerExt;
import grondag.canvas.wip.encoding.WipVertexCollectorImpl;
import grondag.canvas.wip.state.RenderContextState;
import grondag.canvas.wip.state.WipRenderMaterial;
import grondag.canvas.wip.state.WipRenderMaterialFinder;
import grondag.canvas.wip.state.property.WipDecal;
import grondag.canvas.wip.state.property.WipDepthTest;
import grondag.canvas.wip.state.property.WipFog;
import grondag.canvas.wip.state.property.WipTarget;
import grondag.canvas.wip.state.property.WipTransparency;
import grondag.canvas.wip.state.property.WipWriteMask;
import grondag.frex.api.material.MaterialMap;
import grondag.frex.api.material.RenderMaterial;
import org.lwjgl.opengl.GL11;

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

public class CanvasParticleRenderer {
	private final WipVertexCollectorImpl collector;

	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	private LightmapTextureManager lightmapTextureManager;
	private ParticleManagerExt ext;
	private Runnable drawHandler = Runnables.doNothing();
	private WipRenderMaterial baseMat;
	private WipRenderMaterial emissiveMat;

	CanvasParticleRenderer(RenderContextState contextState) {
		collector = new WipVertexCollectorImpl(contextState);
	}

	public void renderParticles(ParticleManager pm, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta) {
		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(matrixStack.peek().getModel());

		this.lightmapTextureManager = lightmapTextureManager;
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

			if (!particles.hasNext()) continue;


			final VertexConsumer consumer = beginSheet(particleTextureSheet);

			while(particles.hasNext()) {
				final Particle particle = particles.next();

				try {
					// FEAT: enhanced material maps for particles - shaders for animation in particular
					final RenderMaterial mat = (RenderMaterial) MaterialMap.getForParticle(((ParticleExt) particle).canvas_particleType()).getMapped(null);
					collector.vertexState(mat == null || !mat.emissive() ? baseMat : emissiveMat);
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
		teardownVanillParticleRender();
	}

	private void setupVanillaParticleRender() {
		lightmapTextureManager.enable();
		RenderSystem.enableAlphaTest();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.enableFog();
	}

	private void teardownVanillParticleRender() {
		RenderSystem.depthMask(true);
		RenderSystem.depthFunc(515);
		RenderSystem.disableBlend();
		RenderSystem.defaultAlphaFunc();
		lightmapTextureManager.disable();
		RenderSystem.disableFog();
	}

	private VertexConsumer beginSheet(ParticleTextureSheet particleTextureSheet) {
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		// PERF: consolidate these draws
		if (Configurator.enableExperimentalPipeline) {
			if (particleTextureSheet == ParticleTextureSheet.TERRAIN_SHEET) {
				baseMat = RENDER_STATE_TERRAIN;
				emissiveMat = RENDER_STATE_TERRAIN_EMISSIVE;
				collector.prepare(baseMat);
				drawHandler = () -> collector.drawAndClear();
				return collector;
			} else if (particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_LIT || particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE) {
				baseMat = RENDER_STATE_OPAQUE_OR_LIT;
				emissiveMat = RENDER_STATE_OPAQUE_OR_LIT_EMISSIVE;
				collector.prepare(baseMat);
				drawHandler = () -> collector.drawAndClear();
				return collector;
			} else if (particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
				baseMat = RENDER_STATE_TRANSLUCENT;
				emissiveMat = RENDER_STATE_TRANSLUCENT_EMISSIVE;
				collector.prepare(baseMat);
				drawHandler = () -> collector.drawAndClear();
				return collector;
			}
		}

		setupVanillaParticleRender();
		particleTextureSheet.begin(bufferBuilder, ext.canvas_textureManager());
		drawHandler = () -> particleTextureSheet.draw(tessellator);
		return bufferBuilder;
	}

	private static WipRenderMaterialFinder baseFinder() {
		return WipRenderMaterialFinder.threadLocal()
		.primitive(GL11.GL_QUADS)
		.depthTest(WipDepthTest.LEQUAL)
		.cull(false)
		.writeMask(WipWriteMask.COLOR_DEPTH)
		.enableLightmap(true)
		.decal(WipDecal.NONE)
		.target(WipTarget.PARTICLES)
		.lines(false)
		.disableAo(true)
		.disableDiffuse(true)
		.cutout(true)
		.translucentCutout(true)
		.fog(WipFog.BLACK_FOG);
	}

	private static final WipRenderMaterial RENDER_STATE_TERRAIN = baseFinder()
	.texture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
	.transparency(WipTransparency.DEFAULT)
	.find();

	private static final WipRenderMaterial RENDER_STATE_TERRAIN_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_TERRAIN)
	.emissive(true)
	.find();

	// MC has two but they are functionally identical
	private static final WipRenderMaterial RENDER_STATE_OPAQUE_OR_LIT =  baseFinder()
	.transparency(WipTransparency.NONE)
	.texture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
	.find();

	private static final WipRenderMaterial RENDER_STATE_OPAQUE_OR_LIT_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_OPAQUE_OR_LIT)
	.emissive(true)
	.find();

	private static final WipRenderMaterial RENDER_STATE_TRANSLUCENT = baseFinder()
	.transparency(WipTransparency.TRANSLUCENT)
	.texture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
	.find();

	private static final WipRenderMaterial RENDER_STATE_TRANSLUCENT_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_TRANSLUCENT)
	.emissive(true)
	.find();
}
