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

import blue.endless.jankson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import grondag.canvas.pipeline.config.util.JanksonHelper;

public class PipelineDescription {
	public final ResourceLocation id;
	public final String nameKey;
	public final String descriptionKey;
	public final boolean isFabulous;
	public final boolean shadowsEnabled;
	public final boolean coloredLightsEnabled;

	public static PipelineDescription create(ResourceLocation id, ResourceManager rm) {
		final var included = new ObjectOpenHashSet<ResourceLocation>();
		final var reading = new ObjectArrayFIFOQueue<ResourceLocation>();
		final var objects = new ObjectArrayFIFOQueue<JsonObject>();

		PipelineConfigBuilder.loadResources(id, reading, objects, included, rm);

		if (objects.isEmpty()) {
			return null;
		}

		var config = objects.dequeue();

		final String getNameKey = JanksonHelper.asString(config.get("nameKey"));
		final String nameKey = getNameKey == null ? id.toString() : getNameKey;

		final String getDescriptionKey = JanksonHelper.asString(config.get("descriptionKey"));
		final String descriptionKey = getDescriptionKey == null ? "pipeline.no_desc" : getDescriptionKey;

		boolean fabulous = config.containsKey("fabulousTargets");
		boolean shadows = config.containsKey("skyShadows");
		boolean coloredLights = config.containsKey("coloredLights");

		while (!objects.isEmpty()) {
			var obj = objects.dequeue();
			fabulous |= obj.containsKey("fabulousTargets");
			shadows |= obj.containsKey("skyShadows");
			coloredLights |= obj.containsKey("coloredLights");
		}

		return new PipelineDescription(id, nameKey, descriptionKey, fabulous, shadows, coloredLights);
	}

	public PipelineDescription(ResourceLocation id, String nameKey, String descriptionKey, boolean isFabulous, boolean shadowsEnabled, boolean coloredLightsEnabled) {
		this.id = id;
		this.nameKey = nameKey;
		this.descriptionKey = descriptionKey;
		this.isFabulous = isFabulous;
		this.shadowsEnabled = shadowsEnabled;
		this.coloredLightsEnabled = coloredLightsEnabled;
	}

	public String name() {
		return I18n.get(nameKey);
	}

	public String description() {
		return I18n.get(descriptionKey);
	}
}
