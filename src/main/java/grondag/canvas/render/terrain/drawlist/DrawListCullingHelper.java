/*
 * Copyright © Original Authors
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

package grondag.canvas.render.terrain.drawlist;

import net.minecraft.core.BlockPos;

import io.vram.frex.api.model.util.FaceUtil;

import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.shader.data.ShaderDataManager;

public class DrawListCullingHelper {
	final WorldRenderState worldRenderState;

	public DrawListCullingHelper(WorldRenderState worldRenderState) {
		this.worldRenderState = worldRenderState;
	}

	private int upMaxY;
	private int downMinY;
	private int eastMaxX;
	private int westMinX;
	private int southMaxZ;
	private int northMinZ;
	private int shadowFlags = FaceUtil.UNASSIGNED_FLAG;

	public void update() {
		final long packedCameraRegionOrign = worldRenderState.terrainIterator.cameraRegionOrigin();
		final int x = BlockPos.getX(packedCameraRegionOrign) >> 4;
		final int y = BlockPos.getY(packedCameraRegionOrign) >> 4;
		final int z = BlockPos.getZ(packedCameraRegionOrign) >> 4;
		upMaxY = y + 1;
		downMinY = y - 1;
		eastMaxX = x + 1;
		westMinX = x - 1;
		southMaxZ = z + 1;
		northMinZ = z - 1;

		shadowFlags = FaceUtil.UNASSIGNED_FLAG;
		shadowFlags |= ShaderDataManager.skyLightVector.x() > 0 ? FaceUtil.EAST_FLAG : FaceUtil.WEST_FLAG;
		shadowFlags |= ShaderDataManager.skyLightVector.y() > 0 ? FaceUtil.UP_FLAG : FaceUtil.DOWN_FLAG;
		shadowFlags |= ShaderDataManager.skyLightVector.z() > 0 ? FaceUtil.SOUTH_FLAG : FaceUtil.NORTH_FLAG;
	}

	/** Flag 6 (unassigned) will always be set. */
	public int computeVisibleFaceFlags(long packedOriginBlockPos) {
		final int x = BlockPos.getX(packedOriginBlockPos) >> 4;
		final int y = BlockPos.getY(packedOriginBlockPos) >> 4;
		final int z = BlockPos.getZ(packedOriginBlockPos) >> 4;

		int result = FaceUtil.UNASSIGNED_FLAG;

		if (x < eastMaxX) result |= FaceUtil.EAST_FLAG;
		if (x > westMinX) result |= FaceUtil.WEST_FLAG;

		if (y < upMaxY) result |= FaceUtil.UP_FLAG;
		if (y > downMinY) result |= FaceUtil.DOWN_FLAG;

		if (z < southMaxZ) result |= FaceUtil.SOUTH_FLAG;
		if (z > northMinZ) result |= FaceUtil.NORTH_FLAG;

		return result;
	}

	/** Flag 6 (unassigned) will always be set. */
	public int shadowVisibleFaceFlags() {
		return shadowFlags;
	}
}
