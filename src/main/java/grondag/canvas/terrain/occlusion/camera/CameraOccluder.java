/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.terrain.occlusion.camera;

import java.util.function.Consumer;

import net.minecraft.world.phys.Vec3;

import grondag.bitraster.Matrix4L;
import grondag.bitraster.PerspectiveRasterizer;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.terrain.occlusion.base.AbstractOccluder;
import grondag.canvas.terrain.region.RegionPosition;

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
		final Consumer<Matrix4L> viewSetter = m -> copyMatrixF2L(occlusionFrustum.modelMatrix(), m);
		final Consumer<Matrix4L> projSetter = m -> copyMatrixF2L(occlusionFrustum.projectionMatrix(), m);
		return super.prepareScene(viewVersion, cameraPos.x, cameraPos.y, cameraPos.z, viewSetter, projSetter);
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
