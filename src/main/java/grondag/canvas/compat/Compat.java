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

package grondag.canvas.compat;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class Compat {
	private Compat() { }

	public static void init() {
		WorldRenderEvents.START.register(ctx -> {
			LambDynLightsHolder.updateAll.accept(ctx.worldRenderer());
		});

		WorldRenderEvents.AFTER_SETUP.register(ctx -> {
			LitematicaHolder.litematicaTerrainSetup.accept(ctx.frustum());
		});

		WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
			LitematicaHolder.litematicaRenderSolids.accept(ctx.matrixStack());
			SatinHolder.beforeEntitiesRenderEvent.beforeEntitiesRender(ctx.camera(), ctx.frustum(), ctx.tickDelta());
		});

		WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
			GOMLHolder.HANDLER.render(ctx);
			CampanionHolder.HANDLER.render(ctx);
			SatinHolder.onEntitiesRenderedEvent.onEntitiesRendered(ctx.camera(), ctx.frustum(), ctx.tickDelta());
			LitematicaHolder.litematicaEntityHandler.handle(ctx.matrixStack(), ctx.tickDelta());
			DynocapsHolder.handler.render(ctx.profiler(), ctx.matrixStack(), (Immediate) ctx.consumers(), ctx.camera().getPos());
		});

		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ctx -> {
			ClothHolder.clothDebugPreEvent.run();
			BborHolder.render(ctx);
		});

		WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
			JustMapHolder.justMapRender.renderWaypoints(ctx.matrixStack(), ctx.camera(), ctx.tickDelta());
			LitematicaHolder.litematicaRenderTranslucent.accept(ctx.matrixStack());
			LitematicaHolder.litematicaRenderOverlay.accept(ctx.matrixStack());
			final Vec3d cameraPos = ctx.camera().getPos();
			VoxelMapHolder.postRenderLayerHandler.render(ctx.worldRenderer(), RenderLayer.getTranslucent(), ctx.matrixStack(), cameraPos.getX(), cameraPos.getY(), cameraPos.getZ());

			// litematica overlay uses fabulous buffers so must run before translucent shader when active
			// and also expects view matrix to be pre-applied because it normally happens in weather render
			if (ctx.advancedTranslucency()) {
				RenderSystem.pushMatrix();
				RenderSystem.multMatrix(ctx.matrixStack().peek().getModel());
				MaliLibHolder.litematicaRenderWorldLast.render(ctx.matrixStack(), MinecraftClient.getInstance(), ctx.tickDelta());
				RenderSystem.popMatrix();
			}
		});

		WorldRenderEvents.LAST.register(ctx -> {
			BborHolder.deferred();
			SatinHolder.onWorldRenderedEvent.onWorldRendered(ctx.matrixStack(), ctx.camera(), ctx.tickDelta(), ctx.limitTime());

			// litematica overlay expects to render on top of translucency when fabulous is off
			if (!ctx.advancedTranslucency()) {
				MaliLibHolder.litematicaRenderWorldLast.render(ctx.matrixStack(), MinecraftClient.getInstance(), ctx.tickDelta());
			}
		});

		WorldRenderEvents.END.register(ctx -> {
			VoxelMapHolder.postRenderHandler.render(ctx.worldRenderer(), ctx.matrixStack(), ctx.tickDelta(), ctx.limitTime(), ctx.blockOutlines(), ctx.camera(), ctx.gameRenderer(), ctx.lightmapTextureManager(), ctx.projectionMatrix());
		});

		InvalidateRenderStateCallback.EVENT.register(() -> {
			LitematicaHolder.litematicaReload.run();
		});
	}
}
