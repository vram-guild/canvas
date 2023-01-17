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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import grondag.bitraster.PackedBox;
import grondag.canvas.CanvasMod;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.perf.Timekeeper.ProfilerGroup;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.terrain.region.RenderRegionStorage;
import grondag.canvas.varia.GFX;

public class WorldRenderDraws {
	static void drawOutline(BufferBuilder bufferBuilder, double x0, double y0, double z0, double x1, double y1, double z1, int color) {
		final int a = (color >>> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).endVertex();

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).endVertex();

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).endVertex();
	}

	static void renderCullBoxes(RenderRegionStorage renderRegionStorage, double cameraX, double cameraY, double cameraZ, float tickDelta) {
		@SuppressWarnings("resource") final Entity entity = Minecraft.getInstance().gameRenderer.getMainCamera().getEntity();

		final HitResult hit = entity.pick(12 * 16, tickDelta, true);

		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		final BlockPos pos = ((BlockHitResult) (hit)).getBlockPos();
		final RenderRegion region = renderRegionStorage.getRegionIfExists(pos);

		if (region == null) {
			return;
		}

		final int[] boxes = region.getBuildState().getOcclusionResult().occlusionData();

		if (boxes == null || boxes.length < RegionOcclusionCalculator.OCCLUSION_RESULT_FIRST_BOX_INDEX) {
			return;
		}

		final Tesselator tessellator = Tesselator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableTexture();
		RenderSystem.disableBlend();
		RenderSystem.disableCull();

		final int cb = boxes[0];
		final int limit = boxes.length;

		final double x = (pos.getX() & ~0xF) - cameraX;
		final double y = (pos.getY() & ~0xF) - cameraY;
		final double z = (pos.getZ() & ~0xF) - cameraZ;

		RenderSystem.lineWidth(6.0F);
		bufferBuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
		final int regionRange = region.origin.occlusionRange();

		drawOutline(bufferBuilder, x + PackedBox.x0(cb), y + PackedBox.y0(cb), z + PackedBox.z0(cb), x + PackedBox.x1(cb), y + PackedBox.y1(cb), z + PackedBox.z1(cb), 0xFFAAAAAA);

		for (int i = RegionOcclusionCalculator.OCCLUSION_RESULT_FIRST_BOX_INDEX; i < limit; ++i) {
			final int b = boxes[i];
			final int range = PackedBox.range(b);

			if (regionRange > range) {
				break;
			}

			drawOutline(bufferBuilder, x + PackedBox.x0(b), y + PackedBox.y0(b), z + PackedBox.z0(b), x + PackedBox.x1(b), y + PackedBox.y1(b), z + PackedBox.z1(b), WorldRenderDraws.rangeColor(range));
		}

		tessellator.end();
		GFX.disableDepthTest();
		RenderSystem.lineWidth(3.0F);
		bufferBuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

		drawOutline(bufferBuilder, x + PackedBox.x0(cb), y + PackedBox.y0(cb), z + PackedBox.z0(cb), x + PackedBox.x1(cb), y + PackedBox.y1(cb), z + PackedBox.z1(cb), 0xFFAAAAAA);

		for (int i = RegionOcclusionCalculator.OCCLUSION_RESULT_FIRST_BOX_INDEX; i < limit; ++i) {
			final int b = boxes[i];
			final int range = PackedBox.range(b);

			if (regionRange > range) {
				break;
			}

			drawOutline(bufferBuilder, x + PackedBox.x0(b), y + PackedBox.y0(b), z + PackedBox.z0(b), x + PackedBox.x1(b), y + PackedBox.y1(b), z + PackedBox.z1(b), WorldRenderDraws.rangeColor(range));
		}

		tessellator.end();

		GFX.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.enableTexture();
		RenderSystem.enableCull();
	}

	static int rangeColor(int range) {
		switch (range) {
			case PackedBox.RANGE_NEAR:
			default:
				return 0x80FF8080;

			case PackedBox.RANGE_MID:
				return 0x80FFFF80;

			case PackedBox.RANGE_FAR:
				return 0x8080FF80;

			case PackedBox.RANGE_EXTREME:
				return 0x808080FF;
		}
	}

	static void profileSwap(ProfilerFiller profiler, ProfilerGroup profilerGroup, String token) {
		profiler.popPush(token);
		Timekeeper.instance.swap(profilerGroup, token);
	}

	static void renderBlockEntitySafely(BlockEntity blockEntity, float tickDelta, PoseStack matrixStack, MultiBufferSource outputConsumer) {
		try {
			Minecraft.getInstance().getBlockEntityRenderDispatcher().render(blockEntity, tickDelta, matrixStack, outputConsumer);
		} catch (final Exception e) {
			if (WorldRenderDraws.CAUGHT_BER_ERRORS.add(blockEntity.getType())) {
				CanvasMod.LOG.warn(String.format("Unhandled exception rendering while rendering BlockEntity %s @ %s.  Stack trace follows. Subsequent errors will be suppressed.",
						BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString(), blockEntity.getBlockPos().toShortString()));

				// Passing this to .(warn) causes "Negative index in crash report handler" spam, so printing separately
				e.printStackTrace();
			}
		}
	}

	private static final ReferenceOpenHashSet<BlockEntityType<?>> CAUGHT_BER_ERRORS = new ReferenceOpenHashSet<>();
}
