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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TranslatableComponent;

public class PipelineOptionsEntry extends TooltipListEntry<Void> {
	private final AbstractButton buttonWidget = new AbstractButton(0, 0, 115, 20, NarratorChatListener.NO_TITLE) {
		@Override
		public void onPress() {
			Minecraft.getInstance().setScreen(PipelineOptionGui.display(ConfigGui.pipeline()));
		}

		@Override
		public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
			setMessage(new TranslatableComponent("config.canvas.value.pipeline_config"));
			super.render(matrices, mouseX, mouseY, delta);
		}

		@Override
		public void updateNarration(NarrationElementOutput builder) {
			// Currently unimplemented
		}
	};

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
