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

package grondag.canvas.buffer.render;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.config.Configurator;

public class TransferBuffers {
	private TransferBuffers() { }

	public enum Config {
		DIRECT,

		MAPPED() {
			@Override
			protected TransferBuffer claim(int byteSize) {
				if (RenderSystem.isOnRenderThread()) {
					return MappedTransferBuffer.RENDER_THREAD_ALLOCATOR.claim(byteSize);
				} else {
					final MappedTransferBuffer result = MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.claim(byteSize);
					return result == null ? OffHeapTransferBuffer.THREAD_SAFE_ALLOCATOR.claim(byteSize) : result;
				}
			}

			@Override
			protected void update() {
				assert RenderSystem.isOnRenderThread();

				if (effectiveConfig == Config.MAPPED) {
					MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.forecastUnmetDemand();

					for (int i = 0; i < BinIndex.BIN_COUNT; ++i) {
						final BinIndex bin = BinIndex.fromIndex(i);
						final int demand = MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.unmetDemandForecast(bin);

						if (demand > 0) {
							for (int j = 0; j < demand; ++j) {
								MappedTransferBuffer buff = MappedTransferBuffer.RENDER_THREAD_ALLOCATOR.take(bin);
								buff.prepareForOffThreadUse();
								MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.put(buff);
							}
						}
					}
				}
			}
		},

		HYBRID() {
			@Override
			protected TransferBuffer claim(int byteSize) {
				if (RenderSystem.isOnRenderThread()) {
					return MappedTransferBuffer.RENDER_THREAD_ALLOCATOR.claim(byteSize);
				} else {
					return OffHeapTransferBuffer.THREAD_SAFE_ALLOCATOR.claim(byteSize);
				}
			}
		},

		AUTO;

		protected TransferBuffer claim(int byteSize) {
			return OffHeapTransferBuffer.THREAD_SAFE_ALLOCATOR.claim(byteSize);
		}

		protected void update() {
			// NOOP
		}
	}

	private static Config effectiveConfig = Configurator.transferBufferMode;

	public static TransferBuffer claim(int byteSize) {
		return effectiveConfig.claim(byteSize);
	}

	public static void update() {
		effectiveConfig.update();
	}

	public static void forceReload() {
		assert RenderSystem.isOnRenderThread();

		effectiveConfig = Configurator.transferBufferMode;

		if (effectiveConfig == Config.AUTO) {
			effectiveConfig = Config.HYBRID;
		}

		MappedTransferBuffer.RENDER_THREAD_ALLOCATOR.forceReload();
		MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.forceReload();
		OffHeapTransferBuffer.THREAD_SAFE_ALLOCATOR.forceReload();
	}

	public static String debugString() {
		return String.format("Peak mapped xfer buffers:%5.1fMb", (double) MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.totalPeakDemandBytes() / 0x100000);
	}
}
