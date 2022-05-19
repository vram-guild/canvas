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

package grondag.canvas.terrain.occlusion.shadow;

import static grondag.bitraster.Constants.DOWN;
import static grondag.bitraster.Constants.EAST;
import static grondag.bitraster.Constants.NORTH;
import static grondag.bitraster.Constants.SOUTH;
import static grondag.bitraster.Constants.UP;
import static grondag.bitraster.Constants.WEST;

import java.util.function.Consumer;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import io.vram.frex.api.math.FastMatrix4f;

import grondag.bitraster.Matrix4L;
import grondag.bitraster.OrthoRasterizer;
import grondag.bitraster.PackedBox;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.shader.data.ShadowMatrixData;
import grondag.canvas.terrain.occlusion.base.AbstractOccluder;
import grondag.canvas.terrain.region.RegionPosition;

public class ShadowOccluder extends AbstractOccluder {
	private final Matrix4f shadowViewMatrix = new Matrix4f();
	private final FastMatrix4f shadowViewMatrixExt = (FastMatrix4f) (Object) shadowViewMatrix;

	public final Matrix4f shadowProjMatrix = new Matrix4f();
	private final FastMatrix4f shadowProjMatrixExt = (FastMatrix4f) (Object) shadowProjMatrix;

	private final Vector3f lastVersionedLightVector = new Vector3f();

	private float maxRegionExtent;
	private float r0, x0, y0, z0, r1, x1, y1, z1, r2, x2, y2, z2, r3, x3, y3, z3;
	private int lastViewVersion;
	private Vec3 lastCameraPos;
	private grondag.bitraster.BoxOccluder.BoxTest clearTest;
	private grondag.bitraster.BoxOccluder.BoxTest occludedTest;
	private grondag.bitraster.BoxOccluder.BoxDraw draw;

	public ShadowOccluder(String rasterName) {
		super(new OrthoRasterizer(), rasterName);
	}

	public void copyState(TerrainFrustum occlusionFrustum) {
		shadowViewMatrixExt.f_set(ShadowMatrixData.shadowViewMatrix);
		shadowProjMatrixExt.f_set(ShadowMatrixData.maxCascadeProjMatrix());
		maxRegionExtent = ShadowMatrixData.regionMaxExtent();
		final float[] cascadeCentersAndRadii = ShadowMatrixData.cascadeCentersAndRadii;
		x0 = cascadeCentersAndRadii[0];
		y0 = cascadeCentersAndRadii[1];
		z0 = cascadeCentersAndRadii[2];
		r0 = cascadeCentersAndRadii[3];

		x1 = cascadeCentersAndRadii[4];
		y1 = cascadeCentersAndRadii[5];
		z1 = cascadeCentersAndRadii[6];
		r1 = cascadeCentersAndRadii[7];

		x2 = cascadeCentersAndRadii[8];
		y2 = cascadeCentersAndRadii[9];
		z2 = cascadeCentersAndRadii[10];
		r2 = cascadeCentersAndRadii[11];

		x3 = cascadeCentersAndRadii[12];
		y3 = cascadeCentersAndRadii[13];
		z3 = cascadeCentersAndRadii[14];
		r3 = cascadeCentersAndRadii[15];

		final boolean invalidateView;
		if (lastViewVersion == occlusionFrustum.viewVersion()) {
			final float lightSourceMovement = 1.0f - lastVersionedLightVector.dot(ShaderDataManager.skyLightVector);
			// big sun movement, about 12 in-game minutes or 200 ticks
			invalidateView = lightSourceMovement > 0.0025f;
		} else {
			invalidateView = false;
		}

		if (invalidateView) lastVersionedLightVector.load(ShaderDataManager.skyLightVector);
		lastViewVersion = invalidateView ? -1 : occlusionFrustum.viewVersion();
		lastCameraPos = occlusionFrustum.lastCameraPos();
	}

	@Override
	public void prepareRegion(RegionPosition origin) {
		super.prepareRegion(origin.getX(), origin.getY(), origin.getZ(), PackedBox.RANGE_MID, origin.shadowDistanceRank());
	}

	private final Consumer<Matrix4L> shadowViewSetter = m -> copyMatrixF2L(shadowViewMatrixExt, m);
	private final Consumer<Matrix4L> shadowProjSetter = m -> copyMatrixF2L(shadowProjMatrixExt, m);

	/**
	 * Check if needs redrawn and prep for redraw if so.
	 * When false, regions should be drawn only if their occluder version is not current.
	 */
	@Override
	public boolean prepareScene() {
		return super.prepareScene(lastViewVersion, lastCameraPos.x, lastCameraPos.y, lastCameraPos.z, shadowViewSetter, shadowProjSetter);
	}

	/**
	 * Smallest cascade on which a region at given origin can cast a shadow.
	 * Returns -1 if not within shadow map.
	 */
	public int cascade(RegionPosition regionPosition) {
		// Compute center position in light space
		final Vector4f lightSpaceRegionCenter = new Vector4f();
		lightSpaceRegionCenter.set(regionPosition.cameraRelativeCenterX(), regionPosition.cameraRelativeCenterY(), regionPosition.cameraRelativeCenterZ(), 1.0f);
		lightSpaceRegionCenter.transform(ShadowMatrixData.shadowViewMatrix);

		final float centerX = lightSpaceRegionCenter.x();
		final float centerY = lightSpaceRegionCenter.y();
		final float centerZ = lightSpaceRegionCenter.z();
		final float extent = maxRegionExtent;

		// <= extent = at least partially in
		// < -extent = fully in
		// > extent not in

		final float dx0 = Math.abs(centerX - x0) - r0;
		final float dy0 = Math.abs(centerY - y0) - r0;
		final float dz0 = (centerZ - z0) + r0;

		//		if (dz0 < -extent) {
		//			System.out.println("regin behind zero cascade");
		//		}

		if (dx0 > extent || dy0 > extent || dz0 < -extent) {
			// not a shadow caster
			return -1;
		}

		final float dx3 = Math.abs(centerX - x3) - r3;
		final float dy3 = Math.abs(centerY - y3) - r3;
		final float dz3 = (centerZ - z3) + r3;

		//		if (dz3 < -extent) {
		//			System.out.println("regin behind 3 cascade");
		//		}

		if (dx3 <= extent && dy3 <= extent && dz3 >= -extent) {
			// At least partially in 3
			return 3;
		}

		final float dx2 = Math.abs(centerX - x2) - r2;
		final float dy2 = Math.abs(centerY - y2) - r2;
		final float dz2 = (centerZ - z2) + r2;

		if (dx2 <= extent && dy2 <= extent && dz2 >= -extent) {
			return 2;
		}

		final float dx1 = Math.abs(centerX - x1) - r1;
		final float dy1 = Math.abs(centerY - y1) - r1;
		final float dz1 = (centerZ - z1) + r1;

		if (dx1 <= extent && dy1 <= extent && dz1 >= -extent) {
			return 1;
		}

		return 0;
	}

	public float maxRegionExtent() {
		return maxRegionExtent;
	}

	/**
	 * Does not ever fuzz, unlike perspective.
	 * {@inheritDoc}
	 */
	@Override
	public boolean isBoxVisible(int packedBox, int fuzz) {
		final int x0 = PackedBox.x0(packedBox);
		final int y0 = PackedBox.y0(packedBox);
		final int z0 = PackedBox.z0(packedBox);
		final int x1 = PackedBox.x1(packedBox);
		final int y1 = PackedBox.y1(packedBox);
		final int z1 = PackedBox.z1(packedBox);

		return clearTest.apply(x0, y0, z0, x1, y1, z1);
	}

	/** True if box is partially or fully occluded. */
	public boolean isBoxOccluded(int packedBox) {
		final int x0 = PackedBox.x0(packedBox);
		final int y0 = PackedBox.y0(packedBox);
		final int z0 = PackedBox.z0(packedBox);
		final int x1 = PackedBox.x1(packedBox);
		final int y1 = PackedBox.y1(packedBox);
		final int z1 = PackedBox.z1(packedBox);

		return occludedTest.apply(x0, y0, z0, x1, y1, z1);
	}

	@Override
	public void occludeBox(int packedBox) {
		final int x0 = PackedBox.x0(packedBox);
		final int y0 = PackedBox.y0(packedBox);
		final int z0 = PackedBox.z0(packedBox);
		final int x1 = PackedBox.x1(packedBox);
		final int y1 = PackedBox.y1(packedBox);
		final int z1 = PackedBox.z1(packedBox);

		draw.apply(x0, y0, z0, x1, y1, z1);
	}

	public void setLightVector(Vector3f skylightVector) {
		int outcome = 0;

		if (!Mth.equal(skylightVector.x(), 0)) {
			outcome |= skylightVector.x() > 0 ? EAST : WEST;
		}

		if (!Mth.equal(skylightVector.z(), 0)) {
			outcome |= skylightVector.z() > 0 ? SOUTH : NORTH;
		}

		if (!Mth.equal(skylightVector.y(), 0)) {
			outcome |= skylightVector.y() > 0 ? UP : DOWN;
		}

		clearTest = partiallyClearTests[outcome];
		occludedTest = partiallyOccludedTests[outcome];
		draw = boxDraws[outcome];
	}
}

