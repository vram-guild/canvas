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
	EMISSIVE(CanvasFrameBufferHacks::debugEmissive),
	EMISSIVE_CASCADE(CanvasFrameBufferHacks::debugEmissiveCascade),
	BLOOM_BLUR(CanvasFrameBufferHacks::debugBlur);

	private final Runnable task;

	private BufferDebug(Runnable task) {
		this.task = task;
	}

	private static BufferDebug current = NORMAL;

	public static boolean shouldSkipBlur() {
		return current ==  EMISSIVE  || current == EMISSIVE_CASCADE;
	}

	public static void advance() {
		final BufferDebug[] values = values();
		current = values[(current.ordinal() + 1) % values.length];
	}

	public static void render() {
		while (CanvasMod.BUFFER_KEY.wasPressed()) {
			advance();
		}

		current.task.run();
	}
}
