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

package grondag.canvas.pipeline.config.util;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;

import grondag.canvas.CanvasMod;

public abstract class AbstractConfig {
	protected final ConfigContext context;

	protected AbstractConfig(ConfigContext context) {
		this.context = context;
	}

	public abstract boolean validate();

	public static boolean assertAndWarn(boolean isOK, String msg) {
		if (!isOK) {
			CanvasMod.LOG.warn(msg);
		}

		return isOK;
	}

	public static boolean assertAndWarn(boolean isOK, String msg, Object... args) {
		return assertAndWarn(isOK, String.format(msg, args));
	}

	protected static String[] readerSamplerNames(ConfigContext ctx, JsonObject config, String programName) {
		if (!config.containsKey("samplers")) {
			return new String[0];
		} else {
			final JsonArray names = config.get(JsonArray.class, "samplers");
			final int limit = names != null ? names.size() : 0;
			final String[] samplerNames = new String[limit];

			for (int i = 0; i < limit; ++i) {
				final String s = ctx.dynamic.getString(names.get(i));

				if (s == null) {
					CanvasMod.LOG.warn(String.format("Sampler name %s (%d of %d) for %s is not a valid string and was skipped.",
							names.get(i).toString(), i, limit, programName));
				} else {
					samplerNames[i] = s;
				}
			}

			return samplerNames;
		}
	}
}
