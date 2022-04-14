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

package grondag.canvas.pipeline.config.option;

import java.util.List;

import blue.endless.jankson.JsonObject;
import dev.lambdaurora.spruceui.option.SpruceSeparatorOption;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.config.builder.OptionSession;
import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;

public class OptionConfig extends AbstractConfig {
	public ResourceLocation includeToken;
	public String categoryKey;
	public List<OptionConfigEntry<?>> entries = new ObjectArrayList<>();

	private final boolean isDuplicate;

	public OptionConfig(ConfigContext ctx, JsonObject config) {
		super(ctx);

		includeToken = JanksonHelper.asIdentifier(config.get("includeToken"));
		categoryKey = JanksonHelper.asString(config.get("categoryKey"));
		isDuplicate = includeToken != null && !ctx.optionIds.add(includeToken);

		JsonObject opts = config.getObject("elements");

		// Compatibility with older keyword
		if (opts == null || opts.isEmpty()) {
			opts = config.getObject("options");
		}

		if (opts != null && !opts.isEmpty()) {
			opts.forEach((key, element) -> {
				if (element instanceof JsonObject) {
					entries.add(OptionConfigEntry.of(ctx, key, (JsonObject) element));
				}
			});
		}
	}

	public void readConfig(JsonObject config) {
		JsonObject input = config.getObject(includeToken.toString());

		if (input == null) {
			input = new JsonObject();
		}

		for (final var e : entries) {
			e.readConfig(input);
		}
	}

	public void writeConfig(JsonObject config) {
		final JsonObject output = new JsonObject();

		for (final var e : entries) {
			e.writeConfig(output);
		}

		config.put(includeToken.toString(), output);
	}

	@Override
	public boolean validate() {
		boolean valid = true;

		valid &= assertAndWarn(includeToken != null, "Invalid pipeline config option - includeToken is missing");
		valid &= assertAndWarn(categoryKey != null && !categoryKey.isEmpty(), "Invalid pipeline config option - categoryKey is missing");
		valid &= assertAndWarn(!entries.isEmpty(), "Invalid pipeline config option - no entries");
		valid &= assertAndWarn(!isDuplicate, "Invalid pipeline config option - duplicate includeToken " + includeToken);

		for (final var e : entries) {
			valid &= e.validate();
		}

		return valid;
	}

	public int addGuiEntries(OptionSession optionSession, SpruceOptionListWidget list) {
		final int index = list.addSingleOptionEntry(new SpruceSeparatorOption(categoryKey, true, null));

		for (final var entry : entries) {
			list.addSingleOptionEntry(entry.buildEntry(optionSession).spruceOption());
		}

		return index;
	}

	public String createSource() {
		final StringBuilder builder = new StringBuilder();
		builder.append("/******************************************************\n");
		builder.append("  Generated from " + includeToken.toString() + "\n");
		builder.append("******************************************************/\n");

		for (final var entry : entries) {
			builder.append(entry.createSource());
		}

		return builder.toString();
	}
}
