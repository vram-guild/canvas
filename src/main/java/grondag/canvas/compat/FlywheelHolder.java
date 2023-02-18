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

package grondag.canvas.compat;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.gl.GlStateTracker;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import com.jozufozu.flywheel.core.crumbling.CrumblingRenderer;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.fabric.event.FlywheelEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.LevelRendererExt;
import grondag.canvas.shader.data.MatrixData;
import io.vram.frex.api.renderloop.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FlywheelHolder {
	private static final FlywheelHandler DEFAULT_HANDLER = new EmptyHandler();
	public static FlywheelHandler handler = DEFAULT_HANDLER;

	static {
		if (FabricLoader.getInstance().isModLoaded("flywheel")) {
			try {
				handler = new CompatibleHandler();
			} catch (final Throwable e) {
				// what are the odds?
				closeHandler(e);
			}
		}
	}

	public static class CompatibleHandler implements FlywheelHandler {
		@Override
		public void beginFrame(ClientLevel level, Camera camera, Frustum frustum) {
			try {
				FlywheelEvents.BEGIN_FRAME.invoker().handleEvent(new BeginFrameEvent(level, camera, frustum));
			} catch (final Throwable ignored) {
			}
		}

		@Override
		public void renderLayer(LevelRenderer renderer, RenderType type, PoseStack stack, double camX, double camY, double camZ) {
			try {
				final LevelRendererExt ext = (LevelRendererExt) renderer;
				RenderBuffers renderBuffers = ext.canvas_bufferBuilders();
				GlStateTracker.State restoreState = GlStateTracker.getRestoreState();
				// RenderLayerEvent computes the View-Projection matrix during construction
				stack.pushPose();
				stack.last().pose().load(MatrixData.viewMatrix);
				var event = new RenderLayerEvent(ext.canvas_world(), type, stack, renderBuffers, camX, camY, camZ);
				stack.popPose();

				FlywheelEvents.RENDER_LAYER.invoker().handleEvent(event);
				restoreState.restore();
				// CanvasMod.LOG.warn("flywheel thing is invoked");
			} catch (final Throwable ignored) {

			}
		}

		@Override
		public void renderCrumbling(ClientLevel level, Camera camera, PoseStack stack) {
			CrumblingRenderer.render(level, camera, stack);
		}

		@Override
		public boolean handleInstancing(BlockEntity blockEntity) {
			Level world = blockEntity.getLevel();
			if (!Backend.canUseInstancing(blockEntity.getLevel())) {
				return false;
			}

			if (InstancedRenderRegistry.canInstance(blockEntity.getType())) {
				InstancedRenderDispatcher.getBlockEntities(world).queueAdd(blockEntity);
			}
			return InstancedRenderRegistry.shouldSkipRender(blockEntity);
		}

		@Override
		public void refresh(ClientLevel level) {
			Backend.refresh();

			FlywheelEvents.RELOAD_RENDERERS.invoker().handleEvent(new ReloadRenderersEvent(level));
		}
	}

	private static void closeHandler(Throwable e) {
		CanvasMod.LOG.warn("Unable to deffer to Flywheel due to exception: ", e);
		CanvasMod.LOG.warn("Subsequent errors will be suppressed");
		handler = DEFAULT_HANDLER;
	}

	public interface FlywheelHandler {
		void beginFrame(ClientLevel level, Camera camera, Frustum frustum);

		default void renderLayer(WorldRenderContext ctx, RenderType type) {
			final var cam = ctx.camera().getPosition();
			renderLayer(ctx.worldRenderer(), type, ctx.poseStack(), cam.x, cam.y, cam.z);
		}

		void renderLayer(LevelRenderer renderer, RenderType type, PoseStack stack, double camX, double camY, double camZ);

		void renderCrumbling(ClientLevel level, Camera camera, PoseStack stack);

		default boolean handleInstancing(BlockEntity blockEntity) {
			return false;
		}
		void refresh(ClientLevel level);
	}

	private static final class EmptyHandler implements FlywheelHandler {

		@Override
		public void beginFrame(ClientLevel level, Camera camera, Frustum frustum) {
		}

		@Override
		public void renderLayer(LevelRenderer renderer, RenderType type, PoseStack stack, double camX, double camY, double camZ) {
		}

		@Override
		public void renderCrumbling(ClientLevel level, Camera camera, PoseStack stack) {
		}

		@Override
		public void refresh(ClientLevel level) {
		}
	}
}
