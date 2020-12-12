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
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.Configurator;
import grondag.canvas.mixinterface.GameRendererExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.terrain.region.BuiltRenderRegion;

/**
 * Plane equation derivations based on:
 * "Fast Extraction of Viewing Frustum Planes from the World- View-Projection Matrix"
 * Gill Gribb, Klaus Hartmann
 * https://www.gamedevs.org/uploads/fast-extraction-viewing-frustum-planes-from-world-view-projection-matrix.pdf
 *
 * <p>AABB test method based on work by Ville Miettinen
 * as described in Real-Time Rendering, Fourth Edition (Page 971). CRC Press.
 * Abbey, Duane C.; Haines, Eric; Hoffman, Naty.
 */
@Environment(EnvType.CLIENT)
public class CanvasFrustum extends Frustum {
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

	private static final float MIN_GAP = 0.0001f;
	private final Matrix4fExt mvpMatrix = (Matrix4fExt) (Object) new Matrix4f();
	private final Matrix4fExt lastProjectionMatrix = (Matrix4fExt) (Object) new Matrix4f();
	private final Matrix4fExt lastModelMatrix = (Matrix4fExt) (Object) new Matrix4f();
	private int viewVersion;
	private int positionVersion;
	private double lastViewX = Float.MAX_VALUE;
	private double lastViewY = Float.MAX_VALUE;
	private double lastViewZ = Float.MAX_VALUE;
	private float lastViewXf = Float.MAX_VALUE;
	private float lastViewYf = Float.MAX_VALUE;
	private float lastViewZf = Float.MAX_VALUE;
	private double lastPositionX = Double.MAX_VALUE;
	private double lastPositionY = Double.MAX_VALUE;
	private double lastPositionZ = Double.MAX_VALUE;
	private long lastCameraBlockPos = Long.MAX_VALUE;
	private float lastCameraPitch = Float.MAX_VALUE;
	private float lastCameraYaw = Float.MAX_VALUE;
	private double fov;

	// NB: distance (w) and subtraction are baked into region extents but must be done for other box tests
	private float leftX, leftY, leftZ, leftW, leftXe, leftYe, leftZe, leftRegionExtent;
	private float rightX, rightY, rightZ, rightW, rightXe, rightYe, rightZe, rightRegionExtent;
	private float topX, topY, topZ, topW, topXe, topYe, topZe, topRegionExtent;
	private float bottomX, bottomY, bottomZ, bottomW, bottomXe, bottomYe, bottomZe, bottomRegionExtent;
	private float nearX, nearY, nearZ, nearW, nearXe, nearYe, nearZe, nearRegionExtent;
	private int viewDistanceSquared;

	public CanvasFrustum() {
		super(dummyMatrix(), dummyMatrix());
	}

	void reload() {
		lastViewX = Float.MAX_VALUE;
		lastViewY = Float.MAX_VALUE;
		lastViewZ = Float.MAX_VALUE;
		lastViewXf = Float.MAX_VALUE;
		lastViewYf = Float.MAX_VALUE;
		lastViewZf = Float.MAX_VALUE;
		lastPositionX = Double.MAX_VALUE;
		lastPositionY = Double.MAX_VALUE;
		lastPositionZ = Double.MAX_VALUE;
		lastCameraBlockPos = Long.MAX_VALUE;
		lastCameraPitch = Float.MAX_VALUE;
		lastCameraYaw = Float.MAX_VALUE;
	}

	private static Matrix4f dummyMatrix() {
		final Matrix4f dummy = new Matrix4f();
		dummy.loadIdentity();
		return dummy;
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

	public Matrix4fExt projectionMatrix() {
		return lastProjectionMatrix;
	}

	public Matrix4fExt modelMatrix() {
		return lastModelMatrix;
	}

	public void copy(CanvasFrustum src) {
		viewVersion = src.viewVersion;
		positionVersion = src.positionVersion;

		lastViewX = src.lastViewX;
		lastViewY = src.lastViewY;
		lastViewZ = src.lastViewZ;

		lastViewXf = src.lastViewXf;
		lastViewYf = src.lastViewYf;
		lastViewZf = src.lastViewZf;

		lastPositionX = src.lastPositionX;
		lastPositionY = src.lastPositionY;
		lastPositionZ = src.lastPositionZ;

		lastCameraBlockPos = src.lastCameraBlockPos;
		lastCameraPitch = src.lastCameraPitch;
		lastCameraYaw = src.lastCameraYaw;

		viewDistanceSquared = src.viewDistanceSquared;

		lastModelMatrix.set(src.lastModelMatrix);
		lastProjectionMatrix.set(src.lastProjectionMatrix);
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

	@SuppressWarnings("resource")
	public void prepare(Matrix4f modelMatrix, float tickDelta, Camera camera) {
		final Vec3d vec = camera.getPos();
		final double x = vec.x;
		final double y = vec.y;
		final double z = vec.z;

		final long cameraBlockPos = camera.getBlockPos().asLong();
		boolean movedOneBlock = false;

		if (cameraBlockPos != lastCameraBlockPos) {
			lastCameraBlockPos = cameraBlockPos;
			movedOneBlock = true;
		} else {
			// could move 1.0 or more diagonally within same block pos
			final double dx = x - lastPositionX;
			final double dy = y - lastPositionY;
			final double dz = z - lastPositionZ;
			movedOneBlock = dx * dx + dy * dy + dz * dz >= 1.0D;
		}

		if (movedOneBlock) {
			++positionVersion;
			lastPositionX = x;
			lastPositionY = y;
			lastPositionZ = z;
		}

		boolean modelMatrixUpdate = false;
		final float cameraPitch = camera.getPitch();
		final float cameraYaw = camera.getYaw();

		// view (rotaion) version changes if moved beyond the configured limit
		// the frustum is padded elsewhere to compensate
		// avoids excessive visibility rebuilds when rotating view
		if (cameraPitch != lastCameraPitch || cameraYaw != lastCameraYaw) {
			final float dPitch = lastCameraPitch - cameraPitch;
			final float dYaw = lastCameraYaw - cameraYaw;
			// divide by two because same division occurs in frustum setup - only half degrees given applies to any edge
			final float paddingFov = Configurator.staticFrustumPadding * 0.5f;
			modelMatrixUpdate = dPitch * dPitch + dYaw * dYaw >= paddingFov * paddingFov;
		}

		if (movedOneBlock || modelMatrixUpdate || !lastProjectionMatrix.matches(occlusionProjMat)) {
			++viewVersion;

			lastViewX = x;
			lastViewY = y;
			lastViewZ = z;

			lastViewXf = (float) x;
			lastViewYf = (float) y;
			lastViewZf = (float) z;

			lastCameraPitch = cameraPitch;
			lastCameraYaw = cameraYaw;

			lastModelMatrix.set(modelMatrix);
			lastProjectionMatrix.set(occlusionProjMat);

			mvpMatrix.loadIdentity();
			mvpMatrix.multiply(lastProjectionMatrix);
			mvpMatrix.multiply(lastModelMatrix);

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

		occlusionProjMat.multiply(Matrix4f.viewboxMatrix(fov + padding, client.getWindow().getFramebufferWidth() / (float) client.getWindow().getFramebufferHeight(), 0.05F, gr.getViewDistance() * 4.0F));
	}

	@Override
	public boolean isVisible(Box box) {
		return isVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	public boolean isVisible(double x0, double y0, double z0, double x1, double y1, double z1) {
		final float hdx = (float) (0.5 * (x1 - x0));
		final float hdy = (float) (0.5 * (y1 - y0));
		final float hdz = (float) (0.5 * (z1 - z0));

		assert hdx > 0;
		assert hdy > 0;
		assert hdz > 0;

		final float cx = (float) x0 + hdx - lastViewXf;
		final float cy = (float) y0 + hdy - lastViewYf;
		final float cz = (float) z0 + hdz - lastViewZf;

		if (cx * leftX + cy * leftY + cz * leftZ + leftW - (hdx * leftXe + hdy * leftYe + hdz * leftZe) > 0) {
			return false;
		}

		if (cx * rightX + cy * rightY + cz * rightZ + rightW - (hdx * rightXe + hdy * rightYe + hdz * rightZe) > 0) {
			return false;
		}

		if (cx * nearX + cy * nearY + cz * nearZ + nearW - (hdx * nearXe + hdy * nearYe + hdz * nearZe) > 0) {
			return false;
		}

		if (cx * topX + cy * topY + cz * topZ + topW - (hdx * topXe + hdy * topYe + hdz * topZe) > 0) {
			return false;
		}

		return !(cx * bottomX + cy * bottomY + cz * bottomZ + bottomW - (hdx * bottomXe + hdy * bottomYe + hdz * bottomZe) > 0);
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

	private void extractPlanes() {
		final Matrix4fExt matrix = mvpMatrix;
		final float a00 = matrix.a00();
		final float a01 = matrix.a01();
		final float a02 = matrix.a02();
		final float a03 = matrix.a03();
		final float a10 = matrix.a10();
		final float a11 = matrix.a11();
		final float a12 = matrix.a12();
		final float a13 = matrix.a13();
		final float a30 = matrix.a30();
		final float a31 = matrix.a31();
		final float a32 = matrix.a32();
		final float a33 = matrix.a33();

		float x = a30 + a00;
		float y = a31 + a01;
		float z = a32 + a02;
		float w = a33 + a03;
		float mag = -MathHelper.fastInverseSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		leftX = x;
		leftY = y;
		leftZ = z;
		leftW = w;
		float xe = Math.abs(x);
		float ye = Math.abs(y);
		float ze = Math.abs(z);
		leftXe = xe;
		leftYe = ye;
		leftZe = ze;
		leftRegionExtent = w - 8 * (xe + ye + ze);

		x = a30 - a00;
		y = a31 - a01;
		z = a32 - a02;
		w = a33 - a03;
		mag = -MathHelper.fastInverseSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		rightX = x;
		rightY = y;
		rightZ = z;
		rightW = w;
		xe = Math.abs(x);
		ye = Math.abs(y);
		ze = Math.abs(z);
		rightXe = xe;
		rightYe = ye;
		rightZe = ze;
		rightRegionExtent = w - 8 * (xe + ye + ze);

		x = a30 - a10;
		y = a31 - a11;
		z = a32 - a12;
		w = a33 - a13;
		mag = -MathHelper.fastInverseSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		topX = x;
		topY = y;
		topZ = z;
		topW = w;
		xe = Math.abs(x);
		ye = Math.abs(y);
		ze = Math.abs(z);
		topXe = xe;
		topYe = ye;
		topZe = ze;
		topRegionExtent = w - 8 * (xe + ye + ze);

		x = a30 + a10;
		y = a31 + a11;
		z = a32 + a12;
		w = a33 + a13;
		mag = -MathHelper.fastInverseSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		bottomX = x;
		bottomY = y;
		bottomZ = z;
		bottomW = w;
		xe = Math.abs(x);
		ye = Math.abs(y);
		ze = Math.abs(z);
		bottomXe = xe;
		bottomYe = ye;
		bottomZe = ze;
		bottomRegionExtent = w - 8 * (xe + ye + ze);

		x = a30 + matrix.a20();
		y = a31 + matrix.a21();
		z = a32 + matrix.a22();
		w = a33 + matrix.a23();
		mag = -MathHelper.fastInverseSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		nearX = x;
		nearY = y;
		nearZ = z;
		nearW = w;
		xe = Math.abs(x);
		ye = Math.abs(y);
		ze = Math.abs(z);
		nearXe = xe;
		nearYe = ye;
		nearZe = ze;
		nearRegionExtent = w - 8 * (xe + ye + ze);
	}
}
