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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.TranslatableComponent;

public class PipelineOptionsEntry extends TooltipListEntry<Void> {
	private final Button buttonWidget = new Button(0, 0, 115, 20, new TranslatableComponent("config.canvas.value.pipeline_config"), b -> Minecraft.getInstance().setScreen(PipelineOptionGui.display(ConfigGui.pipeline())));

	private final List<GuiEventListener> children = ImmutableList.of(buttonWidget);

	@SuppressWarnings("deprecation")
	public PipelineOptionsEntry() {
		super(new TranslatableComponent("config.canvas.value.pipeline_config"), () -> Optional.of(ConfigManager.parse("config.canvas.help.pipeline_config")), false);
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return children;
	}

	@Override
	public Void getValue() {
		return null;
	}

	@Override
	public Optional<Void> getDefaultValue() {
		return null;
	}

	@Override
	public void save() {
		// NOOP
	}

	@SuppressWarnings("resource")
	@Override
	public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
		super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
		buttonWidget.y = y;

		if (Minecraft.getInstance().font.isBidirectional()) {
			buttonWidget.x = x + 150 - buttonWidget.getWidth();
		} else {
			buttonWidget.x = x + entryWidth - 150;
		}

		buttonWidget.render(matrices, mouseX, mouseY, delta);
	}

	@Override
	public List<? extends NarratableEntry> narratables() {
		// Nothing for now
		return Collections.emptyList();
	}
}
