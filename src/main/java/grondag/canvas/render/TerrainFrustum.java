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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.GameRendererExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.terrain.region.BuiltRenderRegion;

@Environment(EnvType.CLIENT)
public class TerrainFrustum extends CanvasFrustum {
	// These are for maintaining a project matrix used by occluder.
	// Updated every frame but not used directly by occlude because of concurrency
	// Occluder uses a copy, below.
	private final MatrixStack projectionStack = new MatrixStack();
	private final MinecraftClient client = MinecraftClient.getInstance();
	private final GameRenderer gr = client.gameRenderer;
	private final GameRendererExt grx = (GameRendererExt) gr;
	private final MatrixStack occlusionsProjStack = new MatrixStack();
	private final Matrix4f occlusionProjMat = occlusionsProjStack.peek().getModel();
	private final Matrix4fExt occlusionProjMatEx = (Matrix4fExt) (Object) occlusionProjMat;

	private int viewDistanceSquared;
	private int viewVersion;
	private int positionVersion;
	private double lastPositionX = Double.MAX_VALUE;
	private double lastPositionY = Double.MAX_VALUE;
	private double lastPositionZ = Double.MAX_VALUE;
	private long lastCameraBlockPos = Long.MAX_VALUE;
	private float lastCameraPitch = Float.MAX_VALUE;
	private float lastCameraYaw = Float.MAX_VALUE;
	private double fov;

	void reload() {
		lastViewX = Double.MAX_VALUE;
		lastViewY = Double.MAX_VALUE;
		lastViewZ = Double.MAX_VALUE;
		lastPositionX = Double.MAX_VALUE;
		lastPositionY = Double.MAX_VALUE;
		lastPositionZ = Double.MAX_VALUE;
		lastCameraBlockPos = Long.MAX_VALUE;
		lastCameraPitch = Float.MAX_VALUE;
		lastCameraYaw = Float.MAX_VALUE;
	}

	/**
	 * Incremented when player moves more than 1 block.
	 * Triggers region visibility recalc and translucency resort.
	 */
	public int positionVersion() {
		return positionVersion;
	}

	/**
	 * Incremented when frustum changes for any reason by any amount - movement, rotation, etc.
	 */
	public int viewVersion() {
		return viewVersion;
	}

	public void copy(TerrainFrustum src) {
		viewVersion = src.viewVersion;
		positionVersion = src.positionVersion;

		lastViewX = src.lastViewX;
		lastViewY = src.lastViewY;
		lastViewZ = src.lastViewZ;

		lastViewX = src.lastViewX;
		lastViewY = src.lastViewY;
		lastViewZ = src.lastViewZ;

		lastPositionX = src.lastPositionX;
		lastPositionY = src.lastPositionY;
		lastPositionZ = src.lastPositionZ;

		lastCameraBlockPos = src.lastCameraBlockPos;
		lastCameraPitch = src.lastCameraPitch;
		lastCameraYaw = src.lastCameraYaw;

		viewDistanceSquared = src.viewDistanceSquared;

		modelMatrixExt.set(src.modelMatrixExt);
		projectionMatrixExt.set(src.projectionMatrixExt);
		mvpMatrixExt.set(src.mvpMatrixExt);

		leftX = src.leftX;
		leftY = src.leftY;
		leftZ = src.leftZ;
		leftW = src.leftW;
		leftXe = src.leftXe;
		leftYe = src.leftYe;
		leftZe = src.leftZe;
		leftRegionExtent = src.leftRegionExtent;

		rightX = src.rightX;
		rightY = src.rightY;
		rightZ = src.rightZ;
		rightW = src.rightW;
		rightXe = src.rightXe;
		rightYe = src.rightYe;
		rightZe = src.rightZe;
		rightRegionExtent = src.rightRegionExtent;

		topX = src.topX;
		topY = src.topY;
		topZ = src.topZ;
		topW = src.topW;
		topXe = src.topXe;
		topYe = src.topYe;
		topZe = src.topZe;
		topRegionExtent = src.topRegionExtent;

		bottomX = src.bottomX;
		bottomY = src.bottomY;
		bottomZ = src.bottomZ;
		bottomW = src.bottomW;
		bottomXe = src.bottomXe;
		bottomYe = src.bottomYe;
		bottomZe = src.bottomZe;
		bottomRegionExtent = src.bottomRegionExtent;

		nearX = src.nearX;
		nearY = src.nearY;
		nearZ = src.nearZ;
		nearW = src.nearW;
		nearXe = src.nearXe;
		nearYe = src.nearYe;
		nearZe = src.nearZe;
		nearRegionExtent = src.nearRegionExtent;

		fov = src.fov;
	}

	@SuppressWarnings("resource")
	public void prepare(Matrix4f modelMatrix, float tickDelta, Camera camera) {
		final Vec3d vec = camera.getPos();
		final double x = vec.x;
		final double y = vec.y;
		final double z = vec.z;

		final long cameraBlockPos = camera.getBlockPos().asLong();
		boolean cameraMoved = false;

		if (cameraBlockPos != lastCameraBlockPos) {
			lastCameraBlockPos = cameraBlockPos;
			cameraMoved = true;
		} else {
			// the assumption that occlusion data only needs updating if the camera moves 1 block doesn't hold up
			final double dx = x - lastPositionX;
			final double dy = y - lastPositionY;
			final double dz = z - lastPositionZ;
			cameraMoved = dx * dx + dy * dy + dz * dz >= 0.01D;
		}

		if (cameraMoved) {
			++positionVersion;
			lastPositionX = x;
			lastPositionY = y;
			lastPositionZ = z;
		}

		boolean modelMatrixUpdate = false;
		final float cameraPitch = camera.getPitch();
		final float cameraYaw = camera.getYaw();

		// view (rotation) version changes if moved beyond the configured limit
		// the frustum is padded elsewhere to compensate
		// avoids excessive visibility rebuilds when rotating view
		if (cameraPitch != lastCameraPitch || cameraYaw != lastCameraYaw) {
			final float dPitch = lastCameraPitch - cameraPitch;
			final float dYaw = lastCameraYaw - cameraYaw;
			// divide by two because same division occurs in frustum setup - only half degrees given applies to any edge
			final float paddingFov = Configurator.staticFrustumPadding * 0.5f;
			modelMatrixUpdate = dPitch * dPitch + dYaw * dYaw >= paddingFov * paddingFov;
		}

		if (cameraMoved || modelMatrixUpdate || !projectionMatrixExt.matches(occlusionProjMat)) {
			++viewVersion;

			lastViewX = x;
			lastViewY = y;
			lastViewZ = z;

			lastCameraPitch = cameraPitch;
			lastCameraYaw = cameraYaw;

			modelMatrixExt.set(modelMatrix);
			projectionMatrixExt.set(occlusionProjMat);

			mvpMatrixExt.loadIdentity();
			mvpMatrixExt.multiply(projectionMatrixExt);
			mvpMatrixExt.multiply(modelMatrixExt);

			// depends on mvpMatrix being complete
			extractPlanes();

			viewDistanceSquared = MinecraftClient.getInstance().options.viewDistance * 16;
			viewDistanceSquared *= viewDistanceSquared;
		}
	}

	/** Called by GameRenderer mixin to avoid recomputing fov. */
	public void setFov(double fov) {
		this.fov = fov;
	}

	public void updateProjection(Camera camera, float tickDelta) {
		int fovPadding = Configurator.terrainSetupOffThread ? Configurator.dynamicFrustumPadding : 0;

		// avoid bobbing frust on hurt/nausea to avoid occlusion update - give sufficient padding
		if (client.options.bobView) {
			fovPadding = Math.max(fovPadding, 5);
		}

		if (MathHelper.lerp(tickDelta, client.player.lastNauseaStrength, client.player.nextNauseaStrength) * client.options.distortionEffectScale * client.options.distortionEffectScale > 0) {
			fovPadding = Math.max(fovPadding, 20);
		}

		boolean doDeadRotation = false;

		if (client.getCameraEntity() instanceof LivingEntity) {
			final LivingEntity livingEntity = (LivingEntity) client.getCameraEntity();

			if (livingEntity.isDead()) {
				doDeadRotation = true;
			} else if (livingEntity.hurtTime - tickDelta > 0) {
				fovPadding = Math.max(fovPadding, 20);
			}
		}

		computeProjectionMatrix(camera, tickDelta, Configurator.staticFrustumPadding + fovPadding);

		if (doDeadRotation) {
			grx.canvas_bobViewWhenHurt(projectionStack, tickDelta);
		}
	}

	private void computeProjectionMatrix(Camera camera, float tickDelta, double padding) {
		occlusionProjMat.loadIdentity();

		if (grx.canvas_zoom() != 1.0F) {
			final float zoom = grx.canvas_zoom();
			occlusionProjMatEx.translate(grx.canvas_zoomX(), -grx.canvas_zoomY(), 0.0F);
			occlusionProjMatEx.scale(zoom, zoom, 1.0F);
		}

		// PERF: WHY 4X ON FAR CLIPPING PLANE MOJANG?
		occlusionProjMat.multiply(Matrix4f.viewboxMatrix(fov + padding, client.getWindow().getFramebufferWidth() / (float) client.getWindow().getFramebufferHeight(), 0.05F, gr.getViewDistance() * 4.0F));
	}

	public boolean isRegionVisible(BuiltRenderRegion region) {
		final float cx = region.cameraRelativeCenterX;
		final float cy = region.cameraRelativeCenterY;
		final float cz = region.cameraRelativeCenterZ;

		if (cx * leftX + cy * leftY + cz * leftZ + leftRegionExtent > MIN_GAP) {
			return false;
		}

		if (cx * rightX + cy * rightY + cz * rightZ + rightRegionExtent > MIN_GAP) {
			return false;
		}

		if (cx * nearX + cy * nearY + cz * nearZ + nearRegionExtent > MIN_GAP) {
			return false;
		}

		if (cx * topX + cy * topY + cz * topZ + topRegionExtent > MIN_GAP) {
			return false;
		}

		return !(cx * bottomX + cy * bottomY + cz * bottomZ + bottomRegionExtent > MIN_GAP);
	}
}
