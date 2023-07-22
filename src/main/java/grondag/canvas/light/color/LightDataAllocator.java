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

import it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortStack;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.shader.data.IntData;

public class LightDataAllocator {
	private static final boolean DEBUG_LOG_RESIZE = false;

	// unsigned short hard limit, because we are putting addresses in the data texture.
	// although, this number of maximum address correspond to about 1 GiB of memory
	// of light data, so this is a reasonable "hard limit".
	private static final int MAX_ADDRESSES = 65536;

	// size of one row as determined by the data size of one region (16^3).
	// also represents row size of pointer header because we are putting addresses in the data texture.
	private static final int ROW_SIZE = LightRegionData.Const.SIZE3D;

	// address for the static empty region.
	static final short EMPTY_ADDRESS = 0;

	private static final int INITIAL_ADDRESS_COUNT = 128;

	private int pointerRows;
	// note: pointer extent always cover the entire view distance, unlike light data manager extent
	private int pointerExtent = -1;
	private boolean needUploadPointers = false;
	private boolean needUploadMeta = false;
	private ByteBuffer pointerBuffer;
	private boolean requireTextureRemake;

	private int dynamicMaxAddresses = 0;
	// 0 is reserved for empty address
	private int nextAllocateAddress = 1;
	private int addressCount = 1;

	private float dataSize = 0f;

	private final Short2LongOpenHashMap allocatedAddresses = new Short2LongOpenHashMap();
	private final ShortStack freedAddresses = new ShortArrayList();

	LightDataAllocator() {
		increaseAddressSize(INITIAL_ADDRESS_COUNT);
	}

	private void resizePointerBuffer(int newPointerExtent) {
		if (newPointerExtent == pointerExtent) {
			return;
		}

		pointerExtent = newPointerExtent;

		final int pointerCountReq = pointerExtent * pointerExtent * pointerExtent;
		var prevRows = pointerRows;
		pointerRows = pointerCountReq / ROW_SIZE + ((pointerCountReq % ROW_SIZE == 0) ? 0 : 1);

		if (DEBUG_LOG_RESIZE) {
			CanvasMod.LOG.info("Resized pointer storage capacity from " + prevRows + " to " + pointerRows);
		}

		if (pointerBuffer != null) {
			pointerBuffer.position(0);
			MemoryUtil.memFree(pointerBuffer);
		}

		pointerBuffer = MemoryUtil.memAlloc(pointerRows * ROW_SIZE * 2);

		// reset each storage value
		while (pointerBuffer.position() < pointerBuffer.limit()) {
			pointerBuffer.putShort((short) 0);
		}

		// remap old pointers
		final var searchPos = new BlockPos.MutableBlockPos();

		for (var entry : allocatedAddresses.short2LongEntrySet()) {
			setPointer(searchPos.set(entry.getLongValue()), entry.getShortKey());
		}

		requireTextureRemake = true;
		needUploadMeta = true;
	}

	// currently, this only increase size
	// TODO: shrink
	private void increaseAddressSize(int newSize) {
		final int cappedNewSize = Math.min(newSize, MAX_ADDRESSES);

		if (dynamicMaxAddresses >= cappedNewSize) {
			return;
		}

		if (DEBUG_LOG_RESIZE) {
			CanvasMod.LOG.info("Resized light data address capacity from " + dynamicMaxAddresses + " to " + cappedNewSize);
		}

		dynamicMaxAddresses = cappedNewSize;

		requireTextureRemake = true;
	}

	int allocateAddress(LightRegion region) {
		if (!region.hasData) {
			return setAddress(region, EMPTY_ADDRESS);
		}

		short regionAddress = region.texAllocation;

		if (regionAddress == EMPTY_ADDRESS) {
			regionAddress = allocateAddressInner(region);
		}

		return regionAddress;
	}

	private short allocateAddressInner(LightRegion region) {
		assert region.hasData;
		final short newAddress;

		// go through freed addresses first
		// note: this is kinda bad when we reach MAX_ADDRESSES, but that's kinda edge case
		if (!freedAddresses.isEmpty()) {
			newAddress = freedAddresses.popShort();
		} else {
			if (addressCount >= dynamicMaxAddresses && addressCount < MAX_ADDRESSES) {
				// try resize first
				increaseAddressSize(dynamicMaxAddresses * 2);
			}

			if (addressCount < dynamicMaxAddresses) {
				newAddress = (short) nextAllocateAddress;
				nextAllocateAddress++;
				addressCount++;
			} else {
				if (nextAllocateAddress >= dynamicMaxAddresses) {
					// rolling pointer
					nextAllocateAddress = EMPTY_ADDRESS + 1;
				}

				final short castedNextAddress = (short) nextAllocateAddress;

				if (allocatedAddresses.containsKey(castedNextAddress)) {
					final long oldOrigin = allocatedAddresses.get(castedNextAddress);
					allocatedAddresses.remove(castedNextAddress);

					final LightRegion oldRegion = LightDataManager.INSTANCE.get(oldOrigin);

					if (oldRegion != null) {
						setAddress(oldRegion, EMPTY_ADDRESS);
					}
				}

				newAddress = castedNextAddress;
				nextAllocateAddress++;
			}
		}

		return setAddress(region, newAddress);
	}

	private short setAddress(LightRegion region, short newAddress) {
		region.texAllocation = newAddress;
		setPointer(region.originPos, newAddress);

		if (newAddress != EMPTY_ADDRESS) {
			allocatedAddresses.put(newAddress, region.origin);
		}

		return newAddress;
	}

	private void setPointer(BlockPos regionOrigin, short regionAddress) {
		final int xInExtent = ((regionOrigin.getX() / 16) % pointerExtent + pointerExtent) % pointerExtent;
		final int yInExtent = ((regionOrigin.getY() / 16) % pointerExtent + pointerExtent) % pointerExtent;
		final int zInExtent = ((regionOrigin.getZ() / 16) % pointerExtent + pointerExtent) % pointerExtent;
		final int pointerIndex = xInExtent * pointerExtent * pointerExtent + yInExtent * pointerExtent + zInExtent;
		final int bufferIndex = pointerIndex * 2;
		final short storedAddress = pointerBuffer.getShort(bufferIndex);
		pointerBuffer.putShort(bufferIndex, regionAddress);
		needUploadPointers |= storedAddress != regionAddress;
	}

	void freeAddress(LightRegion region) {
		if (region.texAllocation != EMPTY_ADDRESS) {
			final short oldAddress = region.texAllocation;
			freedAddresses.push(oldAddress);
			allocatedAddresses.remove(oldAddress);
			setAddress(region, EMPTY_ADDRESS);
		}
	}

	void textureRemade() {
		CanvasMod.LOG.info("Light texture was remade, new size : " + textureHeight());
		requireTextureRemake = false;
		needUploadPointers = true;
		needUploadMeta = true;
		dataSize = (float) (textureWidth() * textureHeight() * 4d / (double) 0x100000);
	}

	public int dataRowStart() {
		return pointerRows;
	}

	public boolean checkInvalid() {
		final var viewDistance = Minecraft.getInstance().options.renderDistance().get();
		final var expectedExtent = (viewDistance + 1) * 2;

		if (pointerExtent < expectedExtent) {
			resizePointerBuffer(expectedExtent);
		}

		return requireTextureRemake;
	}

	public int textureWidth() {
		return ROW_SIZE;
	}

	public int textureHeight() {
		return pointerRows + dynamicMaxAddresses;
	}

	void uploadPointersIfNeeded(LightDataTexture texture) {
		if (needUploadMeta) {
			needUploadMeta = false;
			IntData.UINT_DATA.put(IntData.LIGHT_POINTER_EXTENT, pointerExtent);
			IntData.UINT_DATA.put(IntData.LIGHT_DATA_FIRST_ROW, pointerRows);
		}

		if (needUploadPointers) {
			needUploadPointers = false;
			texture.upload(0, pointerRows, pointerBuffer);
		}
	}

	String debugString() {
		return String.format("Light %s (%d) %d/%d %5.1fMb", Pipeline.config().coloredLights.useOcclusionData ? "LO" : "L", pointerRows, addressCount, dynamicMaxAddresses, dataSize);
	}
}
