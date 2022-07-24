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

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import grondag.canvas.config.builder.Buttons;
import grondag.canvas.config.builder.OptionSession;
import grondag.canvas.config.gui.ActionItem;
import grondag.canvas.config.gui.BaseScreen;
import grondag.canvas.config.gui.ListWidget;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineConfigBuilder;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.pipeline.config.option.OptionConfig;

public class PipelineOptionScreen extends BaseScreen {
	@SuppressWarnings("unused")
	private static final Component EMPTY_TEXT = Component.translatable("config.canvas.category.empty");

	private final ResourceLocation pipelineId;
	private final String pipelineName;
	@SuppressWarnings("unused")
	private final boolean isEmpty;
	private final OptionConfig[] configs;
	private final OptionSession optionSession = new OptionSession();
	private ListWidget list;

	public PipelineOptionScreen(Screen parent, ResourceLocation pipelineId) {
		super(parent, Component.translatable("config.canvas.value.pipeline_config"));
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

		final int sideW = (this.width - 330) >= 72 ? Math.min(120, this.width - 330) : 0;
		final int rightSideW = Math.min(sideW, Math.max(0, this.width - 330 - sideW));

		list = new ListWidget(sideW + 2, 22 + 2, this.width - sideW - 4 - rightSideW, this.height - 35 - 22);
		addRenderableWidget(list);

		list.addItem(new ActionItem(pipelineName, "config.canvas.help.pipeline",
			(x, y, w, h, m, a) -> new Buttons.BrowseButton(x, y, w, h, Component.literal(I18n.get("config.canvas.value.pipeline") + ": " + pipelineName), a),
				() -> minecraft.setScreen(new PipelineSelectionScreen(this))));

		if (configs.length > 0) {
			final ListWidget tabs = new ListWidget(1, list.getY(), sideW, list.getHeight(), true);

			boolean top = true;

			for (final OptionConfig cfg : configs) {
				final int index = cfg.addGuiEntries(optionSession, list);
				final int categoryY = top ? 0 : list.getChildScroll(index);
				top = false;
				tabs.addItem(new ActionItem(cfg.categoryKey, Buttons.SidebarButton::new, () -> list.setScrollAmount(categoryY)));
			}

			if (sideW > 0) {
				addRenderableWidget(tabs);
			}
		}

		final var saveButton = this.addRenderableWidget(new Button(this.width / 2 + 1, this.height - 35 + 6, 120 - 2, 20, CommonComponents.GUI_DONE, b -> save()));
		this.addRenderableWidget(new Button(this.width / 2 - 120 - 1, this.height - 35 + 6, 120 - 2, 20, CommonComponents.GUI_CANCEL, b -> onClose()));

		optionSession.setSaveButton(saveButton);
	}

	private void savePipelineSelection(ResourceLocation newPipelineId) {
		Configurator.pipelineId = newPipelineId.toString();
		// When advanced terrain culling is *soft* disabled, better clear the region storage
		ConfigManager.saveUserInput(Configurator.advancedTerrainCulling ? RELOAD_PIPELINE : RELOAD_EVERYTHING);
	}

	private void save() {
		ConfigManager.savePipelineOptions(configs);
		onClose();
	}

	@Override
	protected void renderTooltips(PoseStack poseStack, int i, int j) {
		if (list != null) {
			final List<FormattedCharSequence> tooltip = list.getTooltip(i, j);

			if (tooltip != null) {
				renderTooltip(poseStack, tooltip, i, j + 30);
			}
		}
	}
}
