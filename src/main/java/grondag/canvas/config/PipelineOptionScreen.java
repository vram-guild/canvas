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

package grondag.canvas.config;

import static grondag.canvas.config.ConfigManager.Reload.RELOAD_EVERYTHING;
import static grondag.canvas.config.ConfigManager.Reload.RELOAD_PIPELINE;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.EmptyBackground;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.option.SpruceSimpleActionOption;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.config.builder.Buttons;
import grondag.canvas.config.builder.OptionSession;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineConfigBuilder;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.pipeline.config.option.OptionConfig;

public class PipelineOptionScreen extends SpruceScreen {
	private static final TranslatableComponent EMPTY_TEXT = new TranslatableComponent("config.canvas.category.empty");

	private final ResourceLocation pipelineId;
	private final String pipelineName;
	private final boolean isEmpty;
	private final OptionConfig[] configs;
	private final Screen parent;

	public PipelineOptionScreen(Screen parent, ResourceLocation pipelineId) {
		super(new TranslatableComponent("config.canvas.value.pipeline_config"));
		this.parent = parent;
		this.pipelineId = pipelineId;

		final PipelineConfig config = PipelineConfigBuilder.build(pipelineId);
		this.pipelineName = I18n.get(PipelineLoader.get(pipelineId.toString()).nameKey);
		this.configs = config.options;
		ConfigManager.initPipelineOptions(configs);

		this.isEmpty = configs.length == 0;
	}

	public void switchBack(ResourceLocation newPipelineId) {
		if (newPipelineId.equals(pipelineId)) {
			minecraft.setScreen(this);
		} else {
			savePipelineSelection(newPipelineId);

			minecraft.setScreen(new PipelineOptionScreen(this.parent, newPipelineId));
		}
	}

	@Override
	protected void init() {
		super.init();

		OptionSession optionSession = new OptionSession();

		Buttons.sideW = (this.width - 330) >= 72 ? Math.min(120, this.width - 330) : 0;
		final int rightSideW = Math.min(Buttons.sideW, Math.max(0, this.width - 330 - Buttons.sideW));

		final SpruceOptionListWidget list = new SpruceOptionListWidget(Position.of(Buttons.sideW + 2, 22 + 2), this.width - Buttons.sideW - 4 - rightSideW, this.height - 35 - 22 - 2);
		list.setBackground(new SimpleColorBackground(0xAA000000));
		addWidget(list);

		list.addSingleOptionEntry(new SpruceSimpleActionOption(pipelineName,
			(p, w, m, a) -> Buttons.browseButton(p, w, new TextComponent(I18n.get("config.canvas.value.pipeline") + ": " + pipelineName), a),
			b -> minecraft.setScreen(new PipelineSelectionScreen(this))));

		if (configs.length > 0) {
			final SpruceOptionListWidget tabs = new SpruceOptionListWidget(Position.of(1, list.getY()), Buttons.sideW, list.getHeight());
			tabs.setBackground(EmptyBackground.EMPTY_BACKGROUND);
			addWidget(tabs);

			boolean top = true;

			for (final OptionConfig cfg : configs) {
				final int index = cfg.addGuiEntries(optionSession, list);
				final int categoryY = top ? 0 : list.children().get(index).getY() - list.getY() - 2;
				top = false;
				tabs.addSingleOptionEntry(new SpruceSimpleActionOption(cfg.categoryKey, Buttons::sideButton, e -> list.setScrollAmount(categoryY)));
			}
		}

		var saveButton = this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 + 1, this.height - 35 + 6), 120 - 2, 20, CommonComponents.GUI_DONE, b -> save()));
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 - 120 - 1, this.height - 35 + 6), 120 - 2, 20, CommonComponents.GUI_CANCEL, b -> close()));

		optionSession.setSaveButton(saveButton);
	}

	private void savePipelineSelection(ResourceLocation newPipelineId) {
		Configurator.pipelineId = newPipelineId.toString();
		// When advanced terrain culling is *soft* disabled, better clear the region storage
		ConfigManager.saveUserInput(Configurator.advancedTerrainCulling ? RELOAD_PIPELINE : RELOAD_EVERYTHING);
	}

	private void save() {
		ConfigManager.savePipelineOptions(configs);
		close();
	}

	private void close() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void renderTitle(PoseStack matrices, int mouseX, int mouseY, float delta) {
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 8, 16777215);

		if (isEmpty) {
			drawCenteredString(matrices, this.font, EMPTY_TEXT, this.width / 2, this.height / 2 - this.font.lineHeight / 2, 16777215);
		}
	}
}
