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

package grondag.canvas.terrain.occlusion.camera;

import grondag.bitraster.PerspectiveRasterizer;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.terrain.occlusion.base.AbstractOccluder;
import grondag.canvas.terrain.region.RegionPosition;
import net.minecraft.world.phys.Vec3;

public class CameraOccluder extends AbstractOccluder {
	/**
	 * This frustum is a snapshot of the view frustum and may lag behind for a frame or two.
	 * A snapshot is used because occlusion test can happen off the main render thread and we
	 * need a stable frustum for each occlusion update.
	 */
	private final TerrainFrustum occlusionFrustum = new TerrainFrustum();

	public CameraOccluder() {
		super(new PerspectiveRasterizer(), "canvas_occlusion_raster.png");
	}

	/**
	 * Synchronizes our frustum snapshot with the input, typically the active terrain view frustum.
	 * Should be called from the main thread when the source is known to be stable and correct.
	 * The snapshot will be used (potentially off thread) for all occlusion tests until the next update.
	 */
	public void copyFrustum(TerrainFrustum source) {
		occlusionFrustum.copy(source);
	}

	public int frustumViewVersion() {
		return occlusionFrustum.viewVersion();
	}

	public int frustumPositionVersion() {
		return occlusionFrustum.occlusionPositionVersion();
	}

	public Vec3 frustumCameraPos() {
		return occlusionFrustum.lastCameraPos();
	}

	@Override
	public void prepareRegion(RegionPosition origin) {
		super.prepareRegion(origin.getX(), origin.getY(), origin.getZ(), origin.occlusionRange(), origin.squaredCameraChunkDistance());
	}

	/**
	 * Check if needs redrawn and prep for redraw if so.
	 * When false, regions should be drawn only if their occluder version is not current.
	 */
	@Override
	public boolean prepareScene() {
		final int viewVersion = occlusionFrustum.viewVersion();
		final Vec3 cameraPos = occlusionFrustum.lastCameraPos();
		final Matrix4fExt projectionMatrix = occlusionFrustum.projectionMatrix();
		final Matrix4fExt modelMatrix = occlusionFrustum.modelMatrix();
		return super.prepareScene(viewVersion, cameraPos.x, cameraPos.y, cameraPos.z, modelMatrix::copyTo, projectionMatrix::copyTo);
	}

	@Override
	public boolean isBoxVisible(int packedBox, int fuzz) {
		return isBoxVisibleFromPerspective(packedBox, fuzz);
	}

	@Override
	public void occludeBox(int packedBox) {
		occludeFromPerspective(packedBox);
	}
}
