/*******************************************************************************
 * Copyright 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.pipeline;

import com.google.common.util.concurrent.Runnables;

import grondag.canvas.CanvasMod;

public enum BufferDebug {
	NORMAL(Runnables.doNothing()),
	NEW_NORMAL(Runnables.doNothing()),
	EMISSIVE(CanvasFrameBufferHacks::debugEmissive),
	EMISSIVE_CASCADE(CanvasFrameBufferHacks::debugEmissiveCascade),
	BLOOM_BLUR(CanvasFrameBufferHacks::debugEmissive),
	BLOOM_BLUR_CASCADE(CanvasFrameBufferHacks::debugEmissiveCascade),
	BLOOM_0(() -> CanvasFrameBufferHacks.debugBlur(0)),
	BLOOM_1(() -> CanvasFrameBufferHacks.debugBlur(1)),
	BLOOM_2(() -> CanvasFrameBufferHacks.debugBlur(2)),
	BLOOM_3(() -> CanvasFrameBufferHacks.debugBlur(3)),
	BLOOM_4(() -> CanvasFrameBufferHacks.debugBlur(4)),
	BLOOM_5(() -> CanvasFrameBufferHacks.debugBlur(5)),
	BLOOM_6(() -> CanvasFrameBufferHacks.debugBlur(6)),
	BLOOM_7(() -> CanvasFrameBufferHacks.debugBlur(7));

	private final Runnable task;

	private BufferDebug(Runnable task) {
		this.task = task;
	}

	private static BufferDebug current = NORMAL;

	public static BufferDebug current() {
		return current;
	}

	public static boolean shouldSkipBlur() {
		return current ==  EMISSIVE  || current == EMISSIVE_CASCADE;
	}

	public static void advance() {
		current  = current == NORMAL ? NEW_NORMAL : NORMAL;
		//		final BufferDebug[] values = values();
		//		current = values[(current.ordinal() + 1) % values.length];
	}

	public static void render() {
		while (CanvasMod.BUFFER_KEY.wasPressed()) {
			advance();
		}

		current.task.run();
	}
}
