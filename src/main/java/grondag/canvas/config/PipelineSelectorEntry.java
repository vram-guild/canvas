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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import grondag.canvas.apiimpl.CanvasState;
import grondag.canvas.pipeline.config.PipelineDescription;
import grondag.canvas.pipeline.config.PipelineLoader;

public class PipelineSelectorEntry extends TooltipListEntry<PipelineDescription> {
	private PipelineDescription value;

	private final AbstractButton buttonWidget;
	private final List<GuiEventListener> children;
	private final PipelineDescription original;

	@SuppressWarnings("deprecation")
	public PipelineSelectorEntry(PipelineDescription initialValue) {
		super(new TranslatableComponent("config.canvas.value.pipeline"), () -> Optional.of(ConfigManager.parse("config.canvas.help.pipeline")), false);

		final PipelineSelectorEntry _this = this;
		buttonWidget = new AbstractButton(0, 0, 115, 20, new TranslatableComponent("config.canvas.value.pipeline_config")) {
			@Override
			public void onPress() {
				Minecraft.getInstance().setScreen(PipelineSelection.display(_this));
			}

			@Override
			public void updateNarration(NarrationElementOutput builder) {
				// Currently unimplemented
			}
		};

		children = ImmutableList.of(buttonWidget);
		original = initialValue;

		setValue(initialValue);
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return children;
	}

	public void setValue(PipelineDescription value) {
		this.value = value;
		this.buttonWidget.setMessage(new TranslatableComponent(value.nameKey));
	}

	@Override
	public PipelineDescription getValue() {
		if (value == null) {
			return PipelineLoader.get(null);
		}

		return value;
	}

	@Override
	public boolean isEdited() {
		return super.isEdited() || getValue() != original;
	}

	@Override
	public Optional<PipelineDescription> getDefaultValue() {
		return Optional.of(PipelineLoader.get(null));
	}

	@Override
	public void save() {
		if (!getValue().id.toString().equals(Configurator.pipelineId)) {
			CanvasState.recompile();
			Configurator.reload = true;
			Configurator.pipelineId = getValue().id.toString();
		}
	}

	@SuppressWarnings("resource")
	@Override
	public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
		super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);

		final Window window = Minecraft.getInstance().getWindow();
		final Component displayedFieldName = this.getDisplayedFieldName();

		if (Minecraft.getInstance().font.isBidirectional()) {
			Minecraft.getInstance().font.drawShadow(matrices, displayedFieldName.getVisualOrderText(), (window.getGuiScaledWidth() - x - Minecraft.getInstance().font.width(displayedFieldName)), y + 6f, 16777215);
			buttonWidget.x = x + 150 - buttonWidget.getWidth();
		} else {
			Minecraft.getInstance().font.drawShadow(matrices, displayedFieldName.getVisualOrderText(), x, y + 6f, this.getPreferredTextColor());
			buttonWidget.x = x + entryWidth - 150;
		}

		buttonWidget.y = y;
		buttonWidget.render(matrices, mouseX, mouseY, delta);
	}

	@Override
	public List<? extends NarratableEntry> narratables() {
		// Nothing for now
		return Collections.emptyList();
	}
}
