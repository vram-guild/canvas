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

package grondag.canvas.render.world;

import java.util.Iterator;

import com.google.common.util.concurrent.Runnables;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;

import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.material.state.MaterialFinderImpl;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.ParticleEngineExt;
import grondag.canvas.mixinterface.ParticleExt;
import grondag.canvas.render.frustum.RegionCullingFrustum;

public class CanvasParticleRenderer {
	private Tesselator tessellator;
	private BufferBuilder bufferBuilder;
	private LightTexture lightmapTextureManager;
	private ParticleEngineExt ext;
	private Runnable drawHandler = Runnables.doNothing();
	private RenderMaterialImpl baseMat;
	private RenderMaterialImpl emissiveMat;
	private final RegionCullingFrustum cullingFrustum;

	public CanvasParticleRenderer(RegionCullingFrustum cullingFrustum) {
		this.cullingFrustum = cullingFrustum;
	}

	public void renderParticles(ParticleEngine pm, PoseStack matrixStack, VertexCollectorList collectors, LightTexture lightmapTextureManager, Camera camera, float tickDelta) {
		cullingFrustum.enableRegionCulling = false;
		final PoseStack renderMatrix = RenderSystem.getModelViewStack();
		renderMatrix.pushPose();
		renderMatrix.mulPoseMatrix(matrixStack.last().pose());
		RenderSystem.applyModelViewMatrix();

		this.lightmapTextureManager = lightmapTextureManager;
		tessellator = Tesselator.getInstance();
		bufferBuilder = tessellator.getBuilder();
		ext = (ParticleEngineExt) pm;
		final Iterator<ParticleRenderType> sheets = ext.canvas_textureSheets().iterator();

		while (sheets.hasNext()) {
			final ParticleRenderType particleTextureSheet = sheets.next();
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
						final var mat = MaterialMap.getForParticle(((ParticleExt) particle).canvas_particleType()).getMapped(null);
						collectors.consumer.defaultMaterial(mat == null || !mat.emissive() ? baseMat : emissiveMat);
					}

					particle.render(consumer, camera, tickDelta);
				} catch (final Throwable exception) {
					final CrashReport crashReport = CrashReport.forThrowable(exception, "Rendering Particle");
					final CrashReportCategory crashReportSection = crashReport.addCategory("Particle being rendered");
					crashReportSection.setDetail("Particle", particle::toString);
					crashReportSection.setDetail("Particle Type", particleTextureSheet::toString);
					throw new ReportedException(crashReport);
				}
			}

			drawHandler.run();
		}

		renderMatrix.popPose();
		RenderSystem.applyModelViewMatrix();
		teardownVanillaParticleRender();
	}

	private void setupVanillaParticleRender() {
		lightmapTextureManager.turnOnLightLayer();
		RenderSystem.enableDepthTest();
	}

	private void teardownVanillaParticleRender() {
		RenderSystem.depthMask(true);
		RenderSystem.depthFunc(515);
		RenderSystem.disableBlend();
		lightmapTextureManager.turnOffLightLayer();
	}

	private VertexConsumer beginSheet(ParticleRenderType particleTextureSheet, VertexCollectorList collectors) {
		RenderSystem.setShader(GameRenderer::getParticleShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		// PERF: consolidate these draws
		if (particleTextureSheet == ParticleRenderType.TERRAIN_SHEET) {
			baseMat = RENDER_STATE_TERRAIN;
			emissiveMat = RENDER_STATE_TERRAIN_EMISSIVE;
			drawHandler = () -> collectors.get(baseMat).draw(true);
			return collectors.consumer.prepare(baseMat);
		} else if (particleTextureSheet == ParticleRenderType.PARTICLE_SHEET_LIT || particleTextureSheet == ParticleRenderType.PARTICLE_SHEET_OPAQUE) {
			baseMat = RENDER_STATE_OPAQUE_OR_LIT;
			emissiveMat = RENDER_STATE_OPAQUE_OR_LIT_EMISSIVE;
			drawHandler = () -> collectors.get(baseMat).draw(true);
			return collectors.consumer.prepare(baseMat);
		} else if (particleTextureSheet == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT) {
			baseMat = RENDER_STATE_TRANSLUCENT;
			emissiveMat = RENDER_STATE_TRANSLUCENT_EMISSIVE;
			drawHandler = () -> collectors.get(baseMat).draw(true);
			return collectors.consumer.prepare(baseMat);
		}

		setupVanillaParticleRender();
		particleTextureSheet.begin(bufferBuilder, ext.canvas_textureManager());
		drawHandler = () -> particleTextureSheet.end(tessellator);
		baseMat = null;
		emissiveMat = null;
		return bufferBuilder;
	}

	private static MaterialFinderImpl baseFinder() {
		return (MaterialFinderImpl) MaterialFinder.threadLocal()
				.depthTest(MaterialConstants.DEPTH_TEST_LEQUAL)
				.cull(false)
				.writeMask(MaterialConstants.WRITE_MASK_COLOR_DEPTH)
				.decal(MaterialConstants.DECAL_NONE)
				.target(MaterialConstants.TARGET_PARTICLES)
				.lines(false)
				.foilOverlay(false)
				.disableAo(true)
				.disableDiffuse(true)
				.cutout(MaterialConstants.CUTOUT_TENTH)
				.fog(true);
	}

	private static final RenderMaterialImpl RENDER_STATE_TERRAIN = baseFinder()
			.texture(TextureAtlas.LOCATION_BLOCKS)
			.transparency(MaterialConstants.TRANSPARENCY_DEFAULT)
			.find();

	private static final RenderMaterialImpl RENDER_STATE_TERRAIN_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_TERRAIN)
			.emissive(true)
			.find();

	// MC has two but they are functionally identical
	private static final RenderMaterialImpl RENDER_STATE_OPAQUE_OR_LIT = baseFinder()
			.transparency(MaterialConstants.TRANSPARENCY_NONE)
			.texture(TextureAtlas.LOCATION_PARTICLES)
			.find();

	private static final RenderMaterialImpl RENDER_STATE_OPAQUE_OR_LIT_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_OPAQUE_OR_LIT)
			.emissive(true)
			.find();

	private static final RenderMaterialImpl RENDER_STATE_TRANSLUCENT = baseFinder()
			.cutout(MaterialConstants.CUTOUT_ZERO)
			.transparency(MaterialConstants.TRANSPARENCY_TRANSLUCENT)
			.texture(TextureAtlas.LOCATION_PARTICLES)
			.find();

	private static final RenderMaterialImpl RENDER_STATE_TRANSLUCENT_EMISSIVE = baseFinder().copyFrom(RENDER_STATE_TRANSLUCENT)
			.emissive(true)
			.find();
}
