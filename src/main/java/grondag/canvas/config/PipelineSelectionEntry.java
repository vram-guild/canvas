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

package grondag.canvas.config;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;

import grondag.canvas.pipeline.config.PipelineDescription;

public class PipelineSelectionEntry extends TooltipListEntry<Boolean> {
	private final AbstractButton buttonWidget;
	private final List<GuiEventListener> children;
	private final PipelineSelection parent;
	private final PipelineDescription pipeline;
	private final boolean original;

	@SuppressWarnings("deprecation")
	public PipelineSelectionEntry(PipelineSelection parent, PipelineDescription description, boolean original) {
		super(new TranslatableComponent(description.nameKey), () -> Optional.of(ConfigManager.parse(description.descriptionKey)), false);

		this.buttonWidget = new AbstractButton(0, 0, 115, 20, this.getFieldName()) {
			@Override
			public void onPress() {
				parent.onSelect(description);
			}

			@Override
			public void render(PoseStack poseStack, int i, int j, float f) {
				if (getValue()) {
					hLine(poseStack, x, x + width - 1, y, 0xFFFFFFFF);
					hLine(poseStack, x, x + width - 1, y + height - 1, 0xFFFFFFFF);
					vLine(poseStack, x, y, y + height, 0xFFFFFFFF);
					vLine(poseStack, x + width - 1, y, y + height, 0xFFFFFFFF);
				}

				@SuppressWarnings("resource")
				final Font font = Minecraft.getInstance().font;
				drawCenteredString(poseStack, font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, 16777215 | Mth.ceil(this.alpha * 255.0F) << 24);
			}

			@Override
			public void updateNarration(NarrationElementOutput builder) {
				// Currently unimplemented
			}
		};

		this.children = ImmutableList.of(buttonWidget);
		this.parent = parent;
		this.pipeline = description;
		this.original = original;
	}

	public PipelineDescription getPipeline() {
		return pipeline;
	}

	@Override
	public boolean isEdited() {
		return super.isEdited() || this.getValue() != this.original;
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return children;
	}

	@Override
	public Boolean getValue() {
		return parent.getValue(this);
	}

	@Override
	public Optional<Boolean> getDefaultValue() {
		return Optional.of(false);
	}

	@Override
	public void save() {
		// NOOP
	}

	@SuppressWarnings("resource")
	@Override
	public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
		super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
		buttonWidget.x = x;
		buttonWidget.y = y;
		buttonWidget.setWidth(entryWidth);
		buttonWidget.render(matrices, mouseX, mouseY, delta);
	}

	@Override
	public List<? extends NarratableEntry> narratables() {
		// Nothing for now
		return Collections.emptyList();
	}
}
