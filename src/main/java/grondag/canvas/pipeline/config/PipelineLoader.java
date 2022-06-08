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

package grondag.canvas.pipeline.config;

import java.io.InputStream;
import java.util.function.Function;

import blue.endless.jankson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.ConfigManager;

public class PipelineLoader {
	private static boolean hasLoadedOnce = false;

	private PipelineLoader() { }

	public static boolean areResourcesAvailable() {
		return hasLoadedOnce;
	}

	public static void reload(ResourceManager manager) {
		hasLoadedOnce = true;
		MAP.clear();

		manager.listResources("pipelines", (location) -> {
			final String stringx = location.toString();
			return stringx.endsWith(".json") || stringx.endsWith(".json5");
		}).forEach((id, resource) -> {
			try (InputStream inputStream = resource.open()) {
				final JsonObject configJson = ConfigManager.JANKSON.load(inputStream);
				final PipelineDescription p = new PipelineDescription(id, configJson);
				MAP.put(id.toString(), p);
			} catch (final Exception e) {
				CanvasMod.LOG.warn(String.format("Unable to load pipeline configuration %s due to unhandled exception.", id), e);
			}
		});
	}

	private static final Object2ObjectOpenHashMap<String, PipelineDescription> MAP = new Object2ObjectOpenHashMap<>();

	public static PipelineDescription get(String idString) {
		if (!MAP.containsKey(idString)) {
			idString = PipelineConfig.DEFAULT_ID.toString();
		}

		return MAP.get(idString);
	}

	public static PipelineDescription[] array() {
		return MAP.values().toArray(new PipelineDescription[MAP.size()]);
	}

	public static final Function<String, Component> NAME_TEXT_FUNCTION = s -> Component.translatable(get(s).nameKey);
}
