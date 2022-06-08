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
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;

import grondag.canvas.config.gui.BaseScreen;
import grondag.canvas.config.gui.ListWidget;
import grondag.canvas.pipeline.config.PipelineDescription;
import grondag.canvas.pipeline.config.PipelineLoader;

public class PipelineSelectionScreen extends BaseScreen {
	private static final Comparator<PipelineDescription> PIPELINE_SORTER = (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name(), o2.name());

	private final PipelineOptionScreen parent;
	private PipelineSelectionEntry selected;
	private ListWidget list;

	protected PipelineSelectionScreen(PipelineOptionScreen parent) {
		super(parent, new TranslatableComponent("config.canvas.category.pipeline_selection"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		final int listW = Math.min(330, this.width);

		list = new ListWidget(this.width / 2 - listW / 2, 22, listW, this.height - 35 - 22);
		final PipelineDescription[] pipelines = PipelineLoader.array();

		Arrays.sort(pipelines, PIPELINE_SORTER);

		for (PipelineDescription e: pipelines) {
			final var entry = new PipelineSelectionEntry(e, this);
			list.addItem(entry);

			if (e.id.toString().equals(Configurator.pipelineId)) {
				changeSelection(entry);
			}
		}

		addRenderableWidget(list);

		this.addRenderableWidget(new Button(this.width / 2 - 120 / 2, this.height - 35 + 6, 120, 20, CommonComponents.GUI_DONE, b -> save()));
	}

	private void save() {
		parent.switchBack(selected.pipeline.id);
	}

	@Override
	public void onClose() {
		save();
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
	protected void renderTooltips(PoseStack poseStack, int i, int j) {
		if (list != null) {
			List<FormattedCharSequence> tooltip = list.getTooltip(i, j);

			if (tooltip != null) {
				renderTooltip(poseStack, tooltip, i, j + 30);
			}
		}
	}
}
