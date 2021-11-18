/*
 * Copyright © Original Authors
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

package grondag.canvas.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineConfigBuilder;
import grondag.canvas.pipeline.config.option.OptionConfig;

public class PipelineOptionGui {
	public static Screen display(ResourceLocation id) {
		final PipelineConfig config = PipelineConfigBuilder.build(id);
		final OptionConfig[] configs = config.options;
		ConfigManager.initPipelineOptions(configs);

		final ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(ConfigGui.current())
				.setTitle(new TranslatableComponent("config.canvas.value.pipeline_config"))
				.setAlwaysShowTabs(false)
				.setShouldListSmoothScroll(true)
				.setDoesConfirmSave(false);

		builder.setGlobalized(true);
		builder.setGlobalizedExpanded(false);

		if (configs.length == 0) {
			builder.setFallbackCategory(builder.getOrCreateCategory(new TranslatableComponent("config.canvas.category.empty")));
		} else {
			for (final OptionConfig cfg : configs) {
				cfg.addGuiEntries(builder, ConfigGui.ENTRY_BUILDER);
			}

			builder.setSavingRunnable(() -> {
				ConfigManager.savePipelineOptions(configs);
			});
		}

		return builder.build();
	}
}
