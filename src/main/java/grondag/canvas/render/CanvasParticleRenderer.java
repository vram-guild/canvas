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

package grondag.canvas.render;

import java.util.Iterator;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.state.MaterialFinderImpl;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.ParticleExt;
import grondag.canvas.mixinterface.ParticleManagerExt;
import grondag.canvas.varia.GFX;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialMap;
import grondag.frex.api.material.RenderMaterial;

public class CanvasParticleRenderer {
	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	private LightmapTextureManager lightmapTextureManager;
	private ParticleManagerExt ext;
	private Runnable drawHandler = Runnables.doNothing();
	private RenderMaterialImpl baseMat;
	private RenderMaterialImpl emissiveMat;
	private final RegionCullingFrustum cullingFrustum;

	public CanvasParticleRenderer(RegionCullingFrustum cullingFrustum) {
		this.cullingFrustum = cullingFrustum;
	}

	public void renderParticles(ParticleManager pm, MatrixStack matrixStack, VertexCollectorList collectors, LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta) {
		cullingFrustum.enableRegionCulling = false;
		final MatrixStack renderMatrix = RenderSystem.getModelViewStack();
		renderMatrix.push();
		renderMatrix.method_34425(matrixStack.peek().getModel());
		RenderSystem.applyModelViewMatrix();

		this.lightmapTextureManager = lightmapTextureManager;
		tessellator = Tessellator.getInstance();
		bufferBuilder = tessellator.getBuffer();
		ext = (ParticleManagerExt) pm;
		final Iterator<ParticleTextureSheet> sheets = ext.canvas_textureSheets().iterator();

		while (sheets.hasNext()) {
			final ParticleTextureSheet particleTextureSheet = sheets.next();
			final Iterable<Particle> iterable = ext.canvas_particles().get(particleTextureSheet);

			if (iterable == null) {
				continue;
			}

			final Iterator<Particle> particles = iterable.iterator();

			if (!particles.hasNext()) continue;

			final VertexConsumer consumer = beginSheet(particleTextureSheet, collectors);

			while (particles.hasNext()) {
				final Particle particle = particles.next();

				if (!cullingFrustum.isVisible(particle.getBoundingBox())) {
					continue;
				}

				try {
					if (baseMat != null) {
						// FEAT: enhanced material maps for particles - shaders for animation in particular
						final RenderMaterial mat = (RenderMaterial) MaterialMap.getForParticle(((ParticleExt) particle).canvas_particleType()).getMapped(null);
						collectors.consumer.defaultMaterial(mat == null || !mat.emissive() ? baseMat : emissiveMat);
					}

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

		renderMatrix.pop();
		RenderSystem.applyModelViewMatrix();
		teardownVanillaParticleRender();
	}

	private void setupVanillaParticleRender() {
		lightmapTextureManager.enable();
		RenderSystem.enableDepthTest();
	}

	private void teardownVanillaParticleRender() {
		RenderSystem.depthMask(true);
		RenderSystem.depthFunc(515);
		RenderSystem.disableBlend();
		lightmapTextureManager.disable();
	}

	private VertexConsumer beginSheet(ParticleTextureSheet particleTextureSheet, VertexCollectorList collectors) {
		RenderSystem.setShader(GameRenderer::getParticleShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		// PERF: consolidate these draws
		if (particleTextureSheet == ParticleTextureSheet.TERRAIN_SHEET) {
			baseMat = RENDER_STATE_TERRAIN;
			emissiveMat = RENDER_STATE_TERRAIN_EMISSIVE;
			drawHandler = () -> collectors.get(baseMat).draw(true);
			return collectors.consumer.prepare(baseMat);
		} else if (particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_LIT || particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE) {
			baseMat = RENDER_STATE_OPAQUE_OR_LIT;
			emissiveMat = RENDER_STATE_OPAQUE_OR_LIT_EMISSIVE;
			drawHandler = () -> collectors.get(baseMat).draw(true);
			return collectors.consumer.prepare(baseMat);
		} else if (particleTextureSheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
			baseMat = RENDER_STATE_TRANSLUCENT;
			emissiveMat = RENDER_STATE_TRANSLUCENT_EMISSIVE;
			drawHandler = () -> collectors.get(baseMat).draw(true);
			return collectors.consumer.prepare(baseMat);
		}

		setupVanillaParticleRender();
		particleTextureSheet.begin(bufferBuilder, ext.canvas_textureManager());
		drawHandler = () -> particleTextureSheet.draw(tessellator);
		baseMat = null;
		emissiveMat = null;
		return bufferBuilder;
	}

	private static MaterialFinderImpl baseFinder() {
		return MaterialFinderImpl.threadLocal()
				.primitive(GFX.GL_QUADS)
				.depthTest(MaterialFinder.DEPTH_TEST_LEQUAL)
				.cull(false)
				.writeMask(MaterialFinder.WRITE_MASK_COLOR_DEPTH)
				.decal(MaterialFinder.DECAL_NONE)
				.target(MaterialFinder.TARGET_PARTICLES)
				.lines(false)
				.enableGlint(false)
				.disableAo(true)
				.disableDiffuse(true)
				.cutout(MaterialFinder.CUTOUT_TENTH)
				.fog(true);
	}

	private static final RenderMaterialImpl RENDER_STATE_TERRAIN = baseFinder()
			.texture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
			.transparency(MaterialFinder.TRANSPARENCY_DEFAULT)
			.find();

	private static final RenderMaterialImpl RENDER_STATE_TERRAIN_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_TERRAIN)
			.emissive(true)
			.find();

	// MC has two but they are functionally identical
	private static final RenderMaterialImpl RENDER_STATE_OPAQUE_OR_LIT = baseFinder()
			.transparency(MaterialFinder.TRANSPARENCY_NONE)
			.texture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
			.find();

	private static final RenderMaterialImpl RENDER_STATE_OPAQUE_OR_LIT_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_OPAQUE_OR_LIT)
			.emissive(true)
			.find();

	private static final RenderMaterialImpl RENDER_STATE_TRANSLUCENT = baseFinder()
			.transparency(MaterialFinder.TRANSPARENCY_TRANSLUCENT)
			.texture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
			.find();

	private static final RenderMaterialImpl RENDER_STATE_TRANSLUCENT_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_TRANSLUCENT)
			.emissive(true)
			.find();
}
