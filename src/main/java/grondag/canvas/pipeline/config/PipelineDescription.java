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

package grondag.canvas.pipeline.config;

import blue.endless.jankson.JsonObject;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.pipeline.config.util.JanksonHelper;

public class PipelineDescription {
	public final ResourceLocation id;
	public final String nameKey;
	public final String descriptionKey;
	public final boolean isFabulous;

	public PipelineDescription (ResourceLocation id, JsonObject config) {
		this.id = id;

		final String nameKey = JanksonHelper.asString(config.get("nameKey"));
		this.nameKey = nameKey == null ? id.toString() : nameKey;
		isFabulous = config.containsKey("fabulousTargets");

		final String descriptionKey = JanksonHelper.asString(config.get("descriptionKey"));
		this.descriptionKey = descriptionKey == null ? "pipeline.no_desc" : descriptionKey;
	}

	public String name() {
		return I18n.get(nameKey);
	}

	public String description() {
		return I18n.get(descriptionKey);
	}
}
