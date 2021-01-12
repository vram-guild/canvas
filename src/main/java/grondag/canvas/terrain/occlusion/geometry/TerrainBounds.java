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

package grondag.canvas.terrain.occlusion.geometry;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.mixinterface.Matrix4fExt;

// WIP: use or remove
public class TerrainBounds {
	private int minX;
	private int minY;
	private int minZ;
	private int maxX;
	private int maxY;
	private int maxZ;

	public TerrainBounds() {
		reset();
	}

	public int minX() {
		return minX;
	}

	public int minY() {
		return minY;
	}

	public int minZ() {
		return minZ;
	}

	public int maxX() {
		return maxX;
	}

	public int maxY() {
		return maxY;
	}

	public int maxZ() {
		return maxZ;
	}

	public void reset() {
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		minZ = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
		maxZ = Integer.MIN_VALUE;
	}

	public void set(TerrainBounds val) {
		minX = val.minX;
		minY = val.minY;
		minZ = val.minZ;
		maxX = val.maxX;
		maxY = val.maxY;
		maxZ = val.maxZ;
	}

	public void addRegion(BlockPos origin) {
		final int x = origin.getX();

		if (x < minX) {
			minX = x;
		}

		if (x + 16 > maxX) {
			maxX = x + 16;
		}

		final int y = origin.getY();

		if (y < minY) {
			minY = y;
		}

		if (y + 16 > maxY) {
			maxY = y + 16;
		}

		final int z = origin.getZ();

		if (z < minZ) {
			minZ = z;
		}

		if (z + 16 > maxZ) {
			maxZ = z + 16;
		}
	}

	private final Vector3f vec = new Vector3f();

	private float minViewX, minViewY, minViewZ, maxViewX, maxViewY, maxViewZ;

	public void computeViewBounds(Matrix4fExt viewMatrix, float cameraX, float cameraY, float cameraZ) {
		vec.set(minX - cameraX, minY - cameraY, minZ - cameraZ);
		viewMatrix.fastTransform(vec);

		minViewX = vec.getX();
		maxViewX = vec.getX();
		minViewY = vec.getY();
		maxViewY = vec.getY();
		minViewZ = vec.getZ();
		maxViewZ = vec.getZ();

		vec.set(minX - cameraX, minY - cameraY, maxZ - cameraZ);
		viewMatrix.fastTransform(vec);
		expandViewBounds();

		vec.set(minX - cameraX, maxY - cameraY, minZ - cameraZ);
		viewMatrix.fastTransform(vec);
		expandViewBounds();

		vec.set(minX - cameraX, maxY - cameraY, maxZ - cameraZ);
		viewMatrix.fastTransform(vec);
		expandViewBounds();

		vec.set(maxX - cameraX, minY - cameraY, minZ - cameraZ);
		viewMatrix.fastTransform(vec);
		expandViewBounds();

		vec.set(maxX - cameraX, minY - cameraY, maxZ - cameraZ);
		viewMatrix.fastTransform(vec);
		expandViewBounds();

		vec.set(maxX - cameraX, maxY - cameraY, minZ - cameraZ);
		viewMatrix.fastTransform(vec);
		expandViewBounds();

		vec.set(maxX - cameraX, maxY - cameraY, maxZ - cameraZ);
		viewMatrix.fastTransform(vec);
		expandViewBounds();
	}

	private void expandViewBounds() {
		final float x = vec.getX();

		if (x < minViewX) {
			minViewX = x;
		} else if (x > maxViewX) {
			maxViewX = x;
		}

		final float y = vec.getY();

		if (y < minViewY) {
			minViewY = y;
		} else if (y > maxViewY) {
			maxViewY = y;
		}

		final float z = vec.getZ();

		if (z < minViewZ) {
			minViewZ = z;
		} else if (z > maxViewZ) {
			maxViewZ = z;
		}
	}

	public float minViewX() {
		return minViewX;
	}

	public float minViewY() {
		return minViewY;
	}

	public float minViewZ() {
		return minViewZ;
	}

	public float maxViewX() {
		return maxViewX;
	}

	public float maxViewY() {
		return maxViewY;
	}

	public float maxViewZ() {
		return maxViewZ;
	}

	public int midX() {
		return (minX + maxX) >> 1;
	}

	public int midY() {
		return (minY + maxY) >> 1;
	}

	public int midZ() {
		return (minZ + maxZ) >> 1;
	}
}
