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

import java.util.Arrays;
import java.util.Comparator;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import grondag.canvas.pipeline.config.PipelineDescription;
import grondag.canvas.pipeline.config.PipelineLoader;

public class PipelineSelectionScreen extends SpruceScreen {
	private static final Comparator<PipelineDescription> PIPELINE_SORTER = (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name(), o2.name());

	private final PipelineOptionScreen parent;
	private PipelineSelectionEntry selected;

	protected PipelineSelectionScreen(PipelineOptionScreen parent) {
		super(new TranslatableComponent("config.canvas.category.pipeline_selection"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		final int listW = Math.min(330, this.width);

		final SpruceOptionListWidget list = new SpruceOptionListWidget(Position.of(this.width / 2 - listW / 2, 22), listW, this.height - 35 - 22);
		final PipelineDescription[] pipelines = PipelineLoader.array();

		Arrays.sort(pipelines, PIPELINE_SORTER);

		for (PipelineDescription e: pipelines) {
			final var entry = new PipelineSelectionEntry(e, this);
			list.addSingleOptionEntry(entry);

			if (e.id.toString().equals(Configurator.pipelineId)) {
				changeSelection(entry);
			}
		}

		list.setBackground(new SimpleColorBackground(0xAA000000));
		addWidget(list);

		// TO-DO Translatable
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 - 80 - 1, this.height - 35 + 6), 80 - 2, 20, new TextComponent("OK"), b -> save()));
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 + 1, this.height - 35 + 6), 80 - 2, 20, new TextComponent("Cancel"), b -> close()));
	}

	private void save() {
		parent.switchBack(selected.pipeline.id);
	}

	private void close() {
		minecraft.setScreen(parent);
	}

	public void onSelect(PipelineSelectionEntry entry) {
		changeSelection(entry);
	}

	private void changeSelection(PipelineSelectionEntry entry) {
		if (selected != null) {
			selected.setSelected(false);
		}

		entry.setSelected(true);

		selected = entry;
	}

	@Override
	public void renderTitle(PoseStack matrices, int mouseX, int mouseY, float delta) {
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 8, 16777215);
	}
}
