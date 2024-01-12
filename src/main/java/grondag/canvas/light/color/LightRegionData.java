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

package grondag.canvas.light.color;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import net.minecraft.core.BlockPos;

class LightRegionData {
	public static class Const {
		public static final int WIDTH = 16;
		public static final int SIZE3D = WIDTH * WIDTH * WIDTH;

		public static final int WIDTH_SHIFT = (int) (Math.log(WIDTH) / Math.log(2));
		public static final int WIDTH_MASK = WIDTH - 1;
	}

	final int regionOriginBlockX;
	final int regionOriginBlockY;
	final int regionOriginBlockZ;
	private ByteBuffer buffer = null;
	private boolean dirty = true;
	private boolean closed = false;

	LightRegionData(int regionOriginBlockX, int regionOriginBlockY, int regionOriginBlockZ) {
		this.regionOriginBlockX = regionOriginBlockX;
		this.regionOriginBlockY = regionOriginBlockY;
		this.regionOriginBlockZ = regionOriginBlockZ;
	}

	private void allocateBuffer() {
		if (buffer != null) {
			throw new IllegalStateException("Trying to allocate light region buffer twice!");
		}

		buffer = MemoryUtil.memAlloc(LightDataTexture.Format.PIXEL_BYTES * Const.SIZE3D);

		// clear manually ?
		while (buffer.position() < LightDataTexture.Format.PIXEL_BYTES * Const.SIZE3D) {
			buffer.putShort((short) 0);
		}
	}

	public void markAsDirty() {
		dirty = true;
	}

	public void clearDirty() {
		dirty = false;
	}

	public void close() {
		if (closed) {
			return;
		}

		if (buffer != null) {
			buffer.position(0);
			// very important
			MemoryUtil.memFree(buffer);
		}

		closed = true;
	}

	public short get(int index) {
		return buffer == null ? 0 : buffer.getShort(index);
	}

	public void put(int index, short light) {
		if (buffer == null) {
			allocateBuffer();
		}

		buffer.putShort(index, light);
	}

	public int indexify(BlockPos pos) {
		return indexify(pos.getX(), pos.getY(), pos.getZ());
	}

	public int indexify(int x, int y, int z) {
		final int localX = x - regionOriginBlockX;
		final int localY = y - regionOriginBlockY;
		final int localZ = z - regionOriginBlockZ;

		// x and z are swapped because opengl
		return ((localZ << (Const.WIDTH_SHIFT * 2)) | (localY << Const.WIDTH_SHIFT) | localX) * LightDataTexture.Format.PIXEL_BYTES;
	}

	public void reverseIndexify(int index, BlockPos.MutableBlockPos result) {
		index = index / LightDataTexture.Format.PIXEL_BYTES;

		// x and z are swapped because opengl
		result.setX((index & Const.WIDTH_MASK) + regionOriginBlockX);
		result.setY(((index >> Const.WIDTH_SHIFT) & Const.WIDTH_MASK) + regionOriginBlockY);
		result.setZ(((index >> Const.WIDTH_SHIFT * 2) & Const.WIDTH_MASK) + regionOriginBlockZ);
	}

	public boolean withinExtents(BlockPos pos) {
		return withinExtents(pos.getX(), pos.getY(), pos.getZ());
	}

	public boolean withinExtents(int x, int y, int z) {
		return (x >= regionOriginBlockX && x < regionOriginBlockX + Const.WIDTH)
				&& (y >= regionOriginBlockY && y < regionOriginBlockY + Const.WIDTH)
				&& (z >= regionOriginBlockZ && z < regionOriginBlockZ + Const.WIDTH);
	}

	boolean hasBuffer() {
		return buffer != null;
	}

	ByteBuffer getBuffer() {
		if (closed || buffer == null) {
			throw new IllegalStateException("Attempting to access a closed or null light region buffer!");
		}

		return buffer;
	}

	public boolean isDirty() {
		return dirty;
	}

	public boolean isClosed() {
		return closed;
	}
}
