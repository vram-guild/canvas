/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.pipeline;

import java.lang.reflect.Field;
import java.util.Locale;

import blue.endless.jankson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.opengl.GL46C;

import grondag.canvas.CanvasMod;

public class GlSymbolLookup {
	private static final Object2IntOpenHashMap<String> MAP = new Object2IntOpenHashMap<>();
	private static final Int2ObjectOpenHashMap<String> REVERSE_MAP = new Int2ObjectOpenHashMap<>();

	static {
		for (final Field f : GL46C.class.getFields()) {
			try {
				final int i = f.getInt(null);
				final String s = f.getName();
				MAP.put(s, i);
				REVERSE_MAP.put(i, s);
			} catch (final Exception e) {
				// eat it
			}
		}
	}

	public static int lookup(String symbol) {
		symbol = symbol.toUpperCase(Locale.ROOT);

		if (!symbol.startsWith("GL_")) {
			symbol = "GL_" + symbol;
		}

		return MAP.getOrDefault(symbol, -1);
	}

	public static String reverseLookup(int id) {
		return REVERSE_MAP.getOrDefault(id, "UNKNOWN");
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
