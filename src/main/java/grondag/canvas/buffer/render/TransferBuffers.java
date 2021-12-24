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
