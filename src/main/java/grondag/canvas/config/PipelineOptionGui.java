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
				.setShouldListSmoothScroll(true)
				.setAlwaysShowTabs(true)
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
