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

import java.util.Arrays;
import java.util.Comparator;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

import grondag.canvas.pipeline.config.PipelineDescription;
import grondag.canvas.pipeline.config.PipelineLoader;

public interface PipelineSelection {
	void onSelect(PipelineDescription entry);

	PipelineDescription getSelected();

	Boolean getValue(PipelineSelectionEntry pipelineSelectionEntry);

	class PipelineSelectionImpl implements PipelineSelection {
		PipelineDescription selected;

		@Override
		public void onSelect(PipelineDescription entry) {
			selected = entry;
		}

		@Override
		public PipelineDescription getSelected() {
			return selected;
		}

		@Override
		public Boolean getValue(PipelineSelectionEntry pipelineSelectionEntry) {
			return selected == pipelineSelectionEntry.getPipeline();
		}
	}

	Comparator<PipelineDescription> PIPELINE_SORTER = (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name(), o2.name());

	static Screen display(PipelineSelectorEntry selector) {
		final PipelineSelection selection = new PipelineSelectionImpl();
		final ConfigBuilder builder = ConfigBuilder.create()
				.setTitle(new TranslatableComponent("config.canvas.value.pipeline"))
				.setParentScreen(ConfigGui.current())
				.setSavingRunnable(() -> selector.setValue(selection.getSelected()))
				.setShouldListSmoothScroll(true)
				.setDoesConfirmSave(false);
		final ConfigCategory category = builder.getOrCreateCategory(new TranslatableComponent("config.canvas.category.pipeline_selection"));
		final PipelineDescription[] sorted = PipelineLoader.array();

		Arrays.sort(sorted, 0, sorted.length, PIPELINE_SORTER);

		for (PipelineDescription pipeline:sorted) {
			final boolean selected = pipeline.id.equals(ConfigGui.pipeline());

			if (selected) {
				selection.onSelect(pipeline);
			}

			final PipelineSelectionEntry entry = new PipelineSelectionEntry(selection, pipeline, selected);

			category.addEntry(entry);
		}

		builder.setGlobalized(true);
		builder.setGlobalizedExpanded(false);

		return builder.build();
	}
}
