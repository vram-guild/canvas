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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.gl.GlStateTracker;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.fabric.event.FlywheelEvents;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;

import io.vram.frex.api.renderloop.WorldRenderContext;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.LevelRendererExt;
import grondag.canvas.shader.data.MatrixData;

// This is heck
public class FlywheelHolder {
	private static final FlywheelHandler DEFAULT_HANDLER = new EmptyHandler();
	public static FlywheelHandler handler = DEFAULT_HANDLER;

	static {
		if (FabricLoader.getInstance().isModLoaded("flywheel")) {
			try {
				compatibleInit(); // boring modCompileOnly method
				// reflectedInit(); // the alternative method
			} catch (final Throwable e) {
				// what are the odds?
				closeHandler(e);
			}
		}
	}

	private static void compatibleInit() {
		handler = new CompatibleHandler();
	}

	// since it's a class it can be debugged comfortably
	private static class CompatibleHandler implements FlywheelHandler {
		@Override
		public void beginFrame(ClientLevel level, Camera camera, Frustum frustum) {
			try {
				FlywheelEvents.BEGIN_FRAME.invoker().handleEvent(new BeginFrameEvent(level, camera, frustum));
			} catch (final Throwable e) {
				closeHandler(e);
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
			} catch (final Throwable e) {
				closeHandler(e);
			}
		}

		@Override
		public void refresh(ClientLevel level) {
			Backend.refresh();
			FlywheelEvents.RELOAD_RENDERERS.invoker().handleEvent(new ReloadRenderersEvent(level));
		}

		@Override
		public boolean handleBEChunkRebuild(BlockEntity be) {
			if (Backend.canUseInstancing(be.getLevel())) {
				if (InstancedRenderRegistry.canInstance(be.getType())) {
					InstancedRenderDispatcher.getBlockEntities(be.getLevel()).queueAdd(be);
				}

				return !InstancedRenderRegistry.shouldSkipRender(be);
			}

			return true;
		}
	}

	// Ultra heck
	private static void reflectedInit() {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			final Class<LevelRenderer> lrClass = LevelRenderer.class;
			final Method renderLayerMethod = lrClass.getDeclaredMethod("flywheel$renderLayer", RenderType.class, PoseStack.class, double.class, double.class, double.class);
			renderLayerMethod.setAccessible(true);
			final MethodHandle renderLayerHandle = lookup.unreflect(renderLayerMethod);

			final Class<?> eventsClass = Class.forName("com.jozufozu.flywheel.fabric.event.FlywheelEvents");
			final Field BEGIN_FRAME = eventsClass.getDeclaredField("BEGIN_FRAME");
			final Field RELOAD_RENDERERS = eventsClass.getDeclaredField("RELOAD_RENDERERS");

			final Class<?> beginFrameEventClass = Class.forName("com.jozufozu.flywheel.event.BeginFrameEvent");
			final Constructor<?> beginFrameEventConstructor = beginFrameEventClass.getDeclaredConstructor(ClientLevel.class, Camera.class, Frustum.class);

			final Class<?> backendClass = Class.forName("com.jozufozu.flywheel.backend.Backend");
			final Method backendRefreshMethod = backendClass.getDeclaredMethod("refresh");
			final MethodHandle refreshHandle = lookup.unreflect(backendRefreshMethod);

			final Class<?> reloadEventClass = Class.forName("com.jozufozu.flywheel.event.ReloadRenderersEvent");
			final Constructor<?> reloadEventConstructor = reloadEventClass.getConstructor(ClientLevel.class);

			final Class<?> contextClass = Class.forName("com.jozufozu.flywheel.fabric.event.EventContext");
			final Class<?> listenerClass = Class.forName("com.jozufozu.flywheel.fabric.event.EventContext$Listener");
			final Method handleEventMethod = listenerClass.getDeclaredMethod("handleEvent", contextClass);
			final MethodHandle handleEventHandle = lookup.unreflect(handleEventMethod);

			handler = new FlywheelHandler() {
				@Override
				public void beginFrame(ClientLevel level, Camera camera, Frustum frustum) {
					try {
						Object listener = listenerClass.cast(((Event<?>) BEGIN_FRAME.get(null)).invoker());
						handleEventHandle.invoke(listener, beginFrameEventConstructor.newInstance(level, camera, frustum));
					} catch (final Throwable e) {
						closeHandler(e);
					}
				}

				@Override
				public void renderLayer(LevelRenderer renderer, RenderType type, PoseStack stack, double camX, double camY, double camZ) {
					try {
						renderLayerHandle.invokeExact(renderer, type, stack, camX, camY, camZ);
						// CanvasMod.LOG.warn("flywheel thing is invoked");
					} catch (final Throwable e) {
						closeHandler(e);
					}
				}

				@Override
				public void refresh(ClientLevel level) {
					try {
						refreshHandle.invokeExact();
						Object listener = listenerClass.cast(((Event<?>) RELOAD_RENDERERS.get(null)).invoker());
						handleEventHandle.invoke(listener, reloadEventConstructor.newInstance(level));
					} catch (final Throwable e) {
						closeHandler(e);
					}
				}

				@Override
				public boolean handleBEChunkRebuild(BlockEntity be) {
					// WIP: implement, or don't use any of this
					return true;
				}
			};

			CanvasMod.LOG.info("Found Flywheel - compatibility hook enabled");
		} catch (Throwable throwable) {
			CanvasMod.LOG.warn("Unable to initialize compatibility for Flywheel due to exception: ", throwable);
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
		void refresh(ClientLevel level);

		boolean handleBEChunkRebuild(BlockEntity be);
	}

	private static final class EmptyHandler implements FlywheelHandler {
		@Override
		public void beginFrame(ClientLevel level, Camera camera, Frustum frustum) {
		}

		@Override
		public void renderLayer(WorldRenderContext ctx, RenderType type) {
		}

		@Override
		public void renderLayer(LevelRenderer renderer, RenderType type, PoseStack stack, double camX, double camY, double camZ) {
		}

		@Override
		public void refresh(ClientLevel level) {
		}

		@Override
		public boolean handleBEChunkRebuild(BlockEntity be) {
			return true;
		}
	}
}
