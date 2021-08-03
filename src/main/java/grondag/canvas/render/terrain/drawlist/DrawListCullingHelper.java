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

package grondag.canvas.render.terrain.drawlist;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.apiimpl.util.FaceConstants;
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
	private int shadowFlags = FaceConstants.UNASSIGNED_FLAG;

	public void update() {
		final long packedCameraRegionOrign = worldRenderState.terrainIterator.cameraRegionOrigin();
		final int x = BlockPos.unpackLongX(packedCameraRegionOrign) >> 4;
		final int y = BlockPos.unpackLongY(packedCameraRegionOrign) >> 4;
		final int z = BlockPos.unpackLongZ(packedCameraRegionOrign) >> 4;
		upMaxY = y + 1;
		downMinY = y - 1;
		eastMaxX = x + 1;
		westMinX = x - 1;
		southMaxZ = z + 1;
		northMinZ = z - 1;

		shadowFlags = FaceConstants.UNASSIGNED_FLAG;
		shadowFlags |= ShaderDataManager.skyLightVector.getX() > 0 ? FaceConstants.EAST_FLAG : FaceConstants.WEST_FLAG;
		shadowFlags |= ShaderDataManager.skyLightVector.getY() > 0 ? FaceConstants.UP_FLAG : FaceConstants.DOWN_FLAG;
		shadowFlags |= ShaderDataManager.skyLightVector.getZ() > 0 ? FaceConstants.SOUTH_FLAG : FaceConstants.NORTH_FLAG;
	}

	/** Flag 6 (unassigned) will always be set. */
	public int computeFlags(long packedOriginBlockPos) {
		final int x = BlockPos.unpackLongX(packedOriginBlockPos) >> 4;
		final int y = BlockPos.unpackLongY(packedOriginBlockPos) >> 4;
		final int z = BlockPos.unpackLongZ(packedOriginBlockPos) >> 4;

		int result = FaceConstants.UNASSIGNED_FLAG;

		if (x < eastMaxX) result |= FaceConstants.EAST_FLAG;
		if (x > westMinX) result |= FaceConstants.WEST_FLAG;

		if (y < upMaxY) result |= FaceConstants.UP_FLAG;
		if (y > downMinY) result |= FaceConstants.DOWN_FLAG;

		if (z < southMaxZ) result |= FaceConstants.SOUTH_FLAG;
		if (z > northMinZ) result |= FaceConstants.NORTH_FLAG;

		return result;
	}

	/** Flag 6 (unassigned) will always be set. */
	public int shadowFlags() {
		return shadowFlags;
	}
}
