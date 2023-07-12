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

import it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortStack;

import net.minecraft.client.Minecraft;

import grondag.canvas.CanvasMod;

public class LightDataAllocator {
	// unsigned short hard limit, because we are putting addresses in the data texture.
	// although, this number of maximum address correspond to about 1 GiB of memory
	// of light data, so this is a reasonable "hard limit".
	private static final int MAX_ADDRESSES = 65536;

	// size of one row as determined by the data size of one region (16^3).
	// also represents row size of pointer header because we are putting addresses in the data texture.
	private static final int ROW_SIZE = 4096;

	// max addresses divided by row size.
	// the number of pointers might exceed number of address, since we assume most just point to empty address.
	private static final int MIN_POINTER_ROWS = 16;

	// address for the static empty region.
	static final short EMPTY_ADDRESS = 0;

	private static final int INITIAL_ADDRESS_COUNT = 128;

	private int pointerHeaderRows;
	private int pointerExtent = -1;
	private boolean requireTextureRemake;

	private int dynamicMaxAddresses = 0;
	// 0 is reserved for empty address
	private int nextAllocateAddress = 1;
	private int addressCount = 1;
	private int debug_prevAddressCount = 0;
	private boolean debug_logResize = true;

	private final Short2LongOpenHashMap allocatedAddresses = new Short2LongOpenHashMap();
	private final ShortStack freedAddresses = new ShortArrayList();

	LightDataAllocator() {
		initPointerHeaderInfo();
		increaseAddressSize(INITIAL_ADDRESS_COUNT);
	}

	private void initPointerHeaderInfo() {
		final int viewDistance = Minecraft.getInstance().options.renderDistance().get();
		final int newPointerExtent = viewDistance * 2;

		if (newPointerExtent == pointerExtent) {
			return;
		}

		final int pointerCountReq = newPointerExtent * newPointerExtent * newPointerExtent;

		pointerExtent = newPointerExtent;
		pointerHeaderRows = Math.max(MIN_POINTER_ROWS, pointerCountReq / ROW_SIZE + (pointerCountReq % ROW_SIZE != 0 ? 1 : 0));
		int maxPointers = pointerHeaderRows * ROW_SIZE;

		if (maxPointers < pointerCountReq) {
			throw new IllegalStateException("Wrong light data allocator size due to logic error!");
		}

		requireTextureRemake = true;
	}

	// currently, this only increase size
	// TODO: shrink
	private void increaseAddressSize(int newSize) {
		final int cappedNewSize = Math.min(newSize, MAX_ADDRESSES);

		if (dynamicMaxAddresses >= cappedNewSize) {
			return;
		}

		if (debug_logResize) {
			CanvasMod.LOG.info("Resized light data address capacity from " + dynamicMaxAddresses + " to " + cappedNewSize);
		}

		dynamicMaxAddresses = cappedNewSize;

		requireTextureRemake = true;
	}

	int allocateAddress(LightRegion region) {
		if (!region.hasData) {
			return EMPTY_ADDRESS;
		}

		int regionAddress = region.texAllocation;

		if (regionAddress == EMPTY_ADDRESS) {
			regionAddress = allocateAddressInner(region);
		}

		// TODO: queue pointer for upload
		final int pointerIndex = getPointerIndex(region);

		return regionAddress;
	}

	private int allocateAddressInner(LightRegion region) {
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
						oldRegion.texAllocation = EMPTY_ADDRESS;
					}
				}

				newAddress = castedNextAddress;
				nextAllocateAddress++;
			}
		}

		region.texAllocation = newAddress;
		allocatedAddresses.put(newAddress, region.origin);

		return newAddress;
	}

	private int getPointerIndex(LightRegion region) {
		final int xInExtent = region.originPos.getX() % pointerExtent;
		final int yInExtent = region.originPos.getY() % pointerExtent;
		final int zInExtent = region.originPos.getZ() % pointerExtent;
		return xInExtent * pointerExtent * pointerExtent + yInExtent * pointerExtent + zInExtent;
	}

	void freeAddress(LightRegion region) {
		if (region.texAllocation != EMPTY_ADDRESS) {
			final short oldAddress = region.texAllocation;
			freedAddresses.push(oldAddress);
			allocatedAddresses.remove(oldAddress);
			region.texAllocation = EMPTY_ADDRESS;
		}
	}

	public void resizeRenderDistance() {
		initPointerHeaderInfo();
	}

	public int dataRowStart() {
		return pointerHeaderRows;
	}

	public boolean requireTextureRemake() {
		return requireTextureRemake;
	}

	public int textureWidth() {
		return ROW_SIZE;
	}

	public int textureHeight() {
		return pointerHeaderRows + dynamicMaxAddresses;
	}

	void uploadPointers() {
		// TODO either return bytebuffer or accept output texture?
	}

	void debug_PrintAddressCount() {
		if (addressCount != debug_prevAddressCount) {
			CanvasMod.LOG.info("Light data allocator address count: " + addressCount);
		}

		debug_prevAddressCount = addressCount;
	}
}
