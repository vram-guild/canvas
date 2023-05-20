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

package grondag.canvas.render.frustum;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import io.vram.frex.api.config.FlawlessFrames;

import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.GameRendererExt;
import grondag.canvas.terrain.region.RegionPosition;

public class TerrainFrustum extends CanvasFrustum {
	// These are for maintaining a project matrix used by occluder.
	// Updated every frame but not used directly by occlude because of concurrency
	// Occluder uses a copy, below.
	private final PoseStack projectionStack = new PoseStack();
	private final Minecraft client = Minecraft.getInstance();
	private final GameRenderer gr = client.gameRenderer;
	private final GameRendererExt grx = (GameRendererExt) gr;
	private final Matrix4f occlusionProjMat = new Matrix4f();

	private int viewDistanceSquared;
	private int viewVersion;
	private int occlusionPositionVersion;
	private double lastOcclusionPositionX = Double.MAX_VALUE;
	private double lastOcclusionPositionY = Double.MAX_VALUE;
	private double lastOcclusionPositionZ = Double.MAX_VALUE;
	private Vec3 lastCameraPos = new Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

	private long lastCameraBlockPos = Long.MAX_VALUE;
	private float lastCameraPitch = Float.MAX_VALUE;
	private float lastCameraYaw = Float.MAX_VALUE;
	private double fov;

	public void reload() {
		lastCameraX = Double.MAX_VALUE;
		lastCameraY = Double.MAX_VALUE;
		lastCameraZ = Double.MAX_VALUE;
		lastOcclusionPositionX = Double.MAX_VALUE;
		lastOcclusionPositionY = Double.MAX_VALUE;
		lastOcclusionPositionZ = Double.MAX_VALUE;
		lastCameraBlockPos = Long.MAX_VALUE;
		lastCameraPitch = Float.MAX_VALUE;
		lastCameraYaw = Float.MAX_VALUE;
		lastCameraPos = new Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
	}

	/**
	 * Incremented when player moves enough to triggers region visibility recalc.
	 */
	public int occlusionPositionVersion() {
		return occlusionPositionVersion;
	}

	/**
	 * Incremented when frustum changes for any reason by any amount - movement, rotation, etc.
	 */
	public int viewVersion() {
		return viewVersion;
	}

	public Vec3 lastCameraPos() {
		return lastCameraPos;
	}

	public void copy(TerrainFrustum src) {
		viewVersion = src.viewVersion;
		occlusionPositionVersion = src.occlusionPositionVersion;

		lastCameraX = src.lastCameraX;
		lastCameraY = src.lastCameraY;
		lastCameraZ = src.lastCameraZ;

		lastCameraX = src.lastCameraX;
		lastCameraY = src.lastCameraY;
		lastCameraZ = src.lastCameraZ;

		lastOcclusionPositionX = src.lastOcclusionPositionX;
		lastOcclusionPositionY = src.lastOcclusionPositionY;
		lastOcclusionPositionZ = src.lastOcclusionPositionZ;

		lastCameraBlockPos = src.lastCameraBlockPos;
		lastCameraPitch = src.lastCameraPitch;
		lastCameraYaw = src.lastCameraYaw;
		lastCameraPos = src.lastCameraPos;

		viewDistanceSquared = src.viewDistanceSquared;

		modelMatrix.set(src.modelMatrix);
		projectionMatrix.set(src.projectionMatrix);
		mvpMatrix.set(src.mvpMatrix);

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

	public void invalidate() {
		viewVersion++;
	}

	@SuppressWarnings("resource")
	public void prepare(Matrix4f viewMatrix, float tickDelta, Camera camera, boolean nearOccludersPresent) {
		final Vec3 cameraPos = camera.getPosition();
		final double x = cameraPos.x;
		final double y = cameraPos.y;
		final double z = cameraPos.z;

		// Ignore near occluders if they aren't occluding!
		nearOccludersPresent &= (Configurator.enableNearOccluders && !FlawlessFrames.isActive());

		final long cameraBlockPos = camera.getBlockPosition().asLong();
		boolean movedEnoughToInvalidateOcclusion = false;

		if (cameraBlockPos != lastCameraBlockPos) {
			lastCameraBlockPos = cameraBlockPos;
			movedEnoughToInvalidateOcclusion = true;
		} else {
			// if no near occluders, assume can move 1.0 or more diagonally within same block pos
			final double dx = x - lastOcclusionPositionX;
			final double dy = y - lastOcclusionPositionY;
			final double dz = z - lastOcclusionPositionZ;
			movedEnoughToInvalidateOcclusion = dx * dx + dy * dy + dz * dz >= (nearOccludersPresent ? 0.0005D : 1.0D);
		}

		if (movedEnoughToInvalidateOcclusion) {
			++occlusionPositionVersion;
			lastOcclusionPositionX = x;
			lastOcclusionPositionY = y;
			lastOcclusionPositionZ = z;
		}

		boolean modelMatrixUpdate = false;
		final float cameraPitch = camera.getXRot();
		final float cameraYaw = camera.getYRot();

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

		final boolean projMatrixUpdate = !projectionMatrix.equals(occlusionProjMat);

		if (movedEnoughToInvalidateOcclusion || modelMatrixUpdate || projMatrixUpdate) {
			++viewVersion;

			lastCameraX = x;
			lastCameraY = y;
			lastCameraZ = z;
			lastCameraPos = cameraPos;
			lastCameraPitch = cameraPitch;
			lastCameraYaw = cameraYaw;

			modelMatrix.set(viewMatrix);
			projectionMatrix.set(occlusionProjMat);

			mvpMatrix.identity();
			mvpMatrix.mul(projectionMatrix);
			mvpMatrix.mul(modelMatrix);

			// depends on mvpMatrix being complete
			extractPlanes();

			viewDistanceSquared = Minecraft.getInstance().options.renderDistance().get() * 16;
			viewDistanceSquared *= viewDistanceSquared;

			// compatibility with mods that expect vanilla frustum
			super.prepare(x, y, z);
			super.calculateFrustum(modelMatrix, projectionMatrix);
		}
	}

	public void updateProjection(Camera camera, float tickDelta, double fov) {
		this.fov = fov;
		int fovPadding = Configurator.terrainSetupOffThread ? Configurator.dynamicFrustumPadding : 0;

		// avoid bobbing frust on hurt/nausea to avoid occlusion update - give sufficient padding
		if (client.options.bobView().get()) {
			fovPadding = Math.max(fovPadding, 5);
		}

		if (Mth.lerp(tickDelta, client.player.oSpinningEffectIntensity, client.player.spinningEffectIntensity) * client.options.screenEffectScale().get() * client.options.screenEffectScale().get() > 0) {
			fovPadding = Math.max(fovPadding, 20);
		}

		boolean doDeadRotation = false;

		if (client.getCameraEntity() instanceof LivingEntity) {
			final LivingEntity livingEntity = (LivingEntity) client.getCameraEntity();

			if (livingEntity.isDeadOrDying()) {
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
		occlusionProjMat.identity();

		if (grx.canvas_zoom() != 1.0F) {
			final float zoom = grx.canvas_zoom();
			occlusionProjMat.translate(grx.canvas_zoomX(), -grx.canvas_zoomY(), 0.0F);
			occlusionProjMat.scale(zoom, zoom, 1.0F);
		}

		// PERF: WHY 4X ON FAR CLIPPING PLANE MOJANG?
		occlusionProjMat.perspective((float) Math.toRadians(fov + padding), (float) client.getWindow().getWidth() / (float) client.getWindow().getHeight(), 0.05F, gr.getRenderDistance() * 4.0F);
	}

	public final RegionVisibilityTest visibilityTest = p -> {
		final float cx = p.cameraRelativeCenterX();
		final float cy = p.cameraRelativeCenterY();
		final float cz = p.cameraRelativeCenterZ();

		if (cx * leftX + cy * leftY + cz * leftZ + leftRegionExtent > 0) {
			return false;
		}

		if (cx * rightX + cy * rightY + cz * rightZ + rightRegionExtent > 0) {
			return false;
		}

		if (cx * nearX + cy * nearY + cz * nearZ + nearRegionExtent > 0) {
			return false;
		}

		if (cx * topX + cy * topY + cz * topZ + topRegionExtent > 0) {
			return false;
		}

		return !(cx * bottomX + cy * bottomY + cz * bottomZ + bottomRegionExtent > 0);
	};

	public interface RegionVisibilityTest {
		boolean isVisible(RegionPosition pos);
	}
}
