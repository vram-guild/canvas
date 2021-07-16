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

package grondag.canvas.buffer.util;

import java.util.concurrent.atomic.AtomicReference;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.buffer.SimpleTransferBufferAllocator;
import grondag.canvas.buffer.TransferBuffer;

public class BinnedAllocationManager {
	public static final int MIN_SIZE = 0x1000;
	public static final int MAX_SIZE = 0x200000;
	private static final int BIN_INDEX_SHIFT = 12;
	private static final int BIN_COUNT = 10;

	private static final AtomicReference<BinState> STATE = new AtomicReference<>(new BinState());

	private static BinState idleState = new BinState();

	static int binIndex(int size) {
		return MathHelper.log2(size >> BIN_INDEX_SHIFT);
	}

	public static class Bin {
		private TransferBuffer claim(int bytes) {
			return SimpleTransferBufferAllocator.claim(bytes);
		}

		private void prepareForUse(Bin activeBin) {
			//
		}

		private void idle() {
			//
		}
	}

	private static class BinState {
		final Bin[] bins = new Bin[BIN_COUNT];

		private void clear() {
			// TODO Auto-generated method stub
		}

		private TransferBuffer claim(int bytes) {
			final int binIndex = binIndex(bytes);
			return binIndex < BIN_COUNT ? bins[binIndex].claim(bytes) : SimpleTransferBufferAllocator.claim(bytes);
		}

		private void prepareForUse(BinState activeState) {
			final Bin[] activeBins = activeState.bins;

			for (int i = 0; i < BIN_COUNT; ++i) {
				bins[i].prepareForUse(activeBins[i]);
			}
		}

		private void idle() {
			for (int i = 0; i < BIN_COUNT; ++i) {
				bins[i].idle();
			}
		}
	}

	public static TransferBuffer claim(int bytes) {
		return STATE.get().claim(bytes);
	}

	public static void forceReload() {
		assert RenderSystem.isOnRenderThread();

		BinState oldState = STATE.getAndSet(new BinState());
		oldState.clear();
	}

	public static String debugString() {
		assert RenderSystem.isOnRenderThread();
		// WIP
		return "";
	}

	public static void update() {
		assert RenderSystem.isOnRenderThread();
		final BinState oldState = STATE.get();
		final BinState newState = idleState;
		newState.prepareForUse(newState);
		STATE.set(newState);
		oldState.idle();
		idleState = oldState;
	}
}
