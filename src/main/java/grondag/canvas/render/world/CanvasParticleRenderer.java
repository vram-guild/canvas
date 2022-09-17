/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import net.minecraft.world.inventory.InventoryMenu;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;

import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.mixinterface.ParticleEngineExt;
import grondag.canvas.mixinterface.ParticleExt;
import grondag.canvas.render.frustum.RegionCullingFrustum;

public class CanvasParticleRenderer {
	private Tesselator tessellator;
	private BufferBuilder bufferBuilder;
	private LightTexture lightmapTextureManager;
	private ParticleEngineExt ext;
	private Runnable drawHandler = Runnables.doNothing();
	private CanvasRenderMaterial baseMat;
	private CanvasRenderMaterial emissiveMat;
	private final RegionCullingFrustum cullingFrustum;
	private final MaterialFinder finder = MaterialFinder.newInstance();

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
						finder.copyFrom(baseMat);
						MaterialMap.getForParticle(((ParticleExt) particle).canvas_particleType()).map(finder, particle);
						// FEAT: enhanced material maps for particles - shaders for animation in particular
						collectors.emitter.defaultMaterial(finder.emissive() ? emissiveMat : baseMat);
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
			return collectors.emitter.prepare(baseMat);
		} else if (particleTextureSheet == ParticleRenderType.PARTICLE_SHEET_LIT || particleTextureSheet == ParticleRenderType.PARTICLE_SHEET_OPAQUE) {
			baseMat = RENDER_STATE_OPAQUE_OR_LIT;
			emissiveMat = RENDER_STATE_OPAQUE_OR_LIT_EMISSIVE;
			drawHandler = () -> collectors.get(baseMat).draw(true);
			return collectors.emitter.prepare(baseMat);
		} else if (particleTextureSheet == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT) {
			baseMat = RENDER_STATE_TRANSLUCENT;
			emissiveMat = RENDER_STATE_TRANSLUCENT_EMISSIVE;
			drawHandler = () -> collectors.get(baseMat).draw(true);
			return collectors.emitter.prepare(baseMat);
		}

		setupVanillaParticleRender();
		particleTextureSheet.begin(bufferBuilder, ext.canvas_textureManager());
		drawHandler = () -> particleTextureSheet.end(tessellator);
		baseMat = null;
		emissiveMat = null;
		return bufferBuilder;
	}

	private static MaterialFinder baseFinder() {
		return MaterialFinder.threadLocal()
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

	private static final CanvasRenderMaterial RENDER_STATE_TERRAIN = (CanvasRenderMaterial) baseFinder()
			.texture(InventoryMenu.BLOCK_ATLAS)
			.transparency(MaterialConstants.TRANSPARENCY_DEFAULT)
			.find();

	private static final CanvasRenderMaterial RENDER_STATE_TERRAIN_EMISSIVE = (CanvasRenderMaterial) baseFinder().copyFrom(RENDER_STATE_TERRAIN)
			.emissive(true)
			.find();

	// MC has two but they are functionally identical
	@SuppressWarnings("deprecation")
	private static final CanvasRenderMaterial RENDER_STATE_OPAQUE_OR_LIT = (CanvasRenderMaterial) baseFinder()
			.transparency(MaterialConstants.TRANSPARENCY_NONE)
			.texture(TextureAtlas.LOCATION_PARTICLES)
			.find();

	private static final CanvasRenderMaterial RENDER_STATE_OPAQUE_OR_LIT_EMISSIVE = (CanvasRenderMaterial) baseFinder().copyFrom(RENDER_STATE_OPAQUE_OR_LIT)
			.emissive(true)
			.find();

	@SuppressWarnings("deprecation")
	private static final CanvasRenderMaterial RENDER_STATE_TRANSLUCENT = (CanvasRenderMaterial) baseFinder()
			.cutout(MaterialConstants.CUTOUT_ZERO)
			.transparency(MaterialConstants.TRANSPARENCY_TRANSLUCENT)
			.texture(TextureAtlas.LOCATION_PARTICLES)
			.find();

	private static final CanvasRenderMaterial RENDER_STATE_TRANSLUCENT_EMISSIVE = (CanvasRenderMaterial) baseFinder().copyFrom(RENDER_STATE_TRANSLUCENT)
			.emissive(true)
			.find();
}
