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

package grondag.canvas.pipeline;

import java.lang.reflect.Field;

import blue.endless.jankson.JsonObject;
import org.lwjgl.opengl.GL46;

import grondag.canvas.CanvasMod;

public class GlSymbolLookup {
	// WIP: cache
	public static int lookup(String symbol) {
		symbol = "GL_" + symbol.toUpperCase();

		try {
			final Field f = GL46.class.getField(symbol);
			return f.getInt(null);
		} catch (final Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static int lookup(JsonObject config, String key, String fallback) {
		String symbol = fallback;

		if (config.containsKey(key)) {
			symbol = config.get(String.class, key);
		}

		int result = GlSymbolLookup.lookup(symbol);

		if (result == -1 && !symbol.equals(fallback)) {
			CanvasMod.LOG.warn(String.format("GL Symbol %s not found, substituting %s", symbol, fallback));
			result = GlSymbolLookup.lookup(fallback);
		}

		return result;
	}
}
