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

package grondag.canvas.pipeline.config;

import java.util.Iterator;
import java.util.function.Function;

import blue.endless.jankson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
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

		final Iterator<?> it = manager.listResources("pipelines", (stringx) -> {
			return stringx.endsWith(".json") || stringx.endsWith(".json5");
		}).iterator();

		while (it.hasNext()) {
			final ResourceLocation id = (ResourceLocation) it.next();

			try (Resource res = manager.getResource(id)) {
				final JsonObject configJson = ConfigManager.JANKSON.load(res.getInputStream());
				final PipelineDescription p = new PipelineDescription(id, configJson);
				MAP.put(id.toString(), p);
			} catch (final Exception e) {
				CanvasMod.LOG.warn(String.format("Unable to load pipeline configuration %s due to unhandled exception.", id), e);
			}
		}
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

	public static final Function<String, Component> NAME_TEXT_FUNCTION = s -> new TranslatableComponent(get(s).nameKey);
}
