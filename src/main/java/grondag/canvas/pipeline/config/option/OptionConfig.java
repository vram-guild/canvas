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

package grondag.canvas.pipeline.config.option;

import java.util.List;

import blue.endless.jankson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;

public class OptionConfig extends AbstractConfig {
	public Identifier includeToken;
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

	public void addGuiEntries(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		final ConfigCategory category = builder.getOrCreateCategory(new TranslatableText(categoryKey));

		for (final var entry : entries) {
			category.addEntry(entry.buildEntry(entryBuilder));
		}
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
