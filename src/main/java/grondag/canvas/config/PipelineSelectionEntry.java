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

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.option.SpruceOption;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.network.chat.TranslatableComponent;

import grondag.canvas.pipeline.config.PipelineDescription;

public class PipelineSelectionEntry extends SpruceOption {
	public final PipelineDescription pipeline;
	private final PipelineSelectionScreen owner;

	private boolean selected = false;
	private SpruceButtonWidget buttonWidget;

	public PipelineSelectionEntry(PipelineDescription pipeline, PipelineSelectionScreen owner) {
		super(pipeline.nameKey);
		this.pipeline = pipeline;
		this.owner = owner;
	}

	void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		final var $this = this;
		this.buttonWidget = new SpruceButtonWidget(position, width, 20, new TranslatableComponent(pipeline.nameKey),
			b -> owner.onSelect($this)) {
			@Override
			public void renderBackground(PoseStack poseStack, int i, int j, float f) {
				final int x = getX();
				final int y = getY();

				if (selected) {
					fill(poseStack, x, y, x + width, y + height - 3, 0x66FFFFFF);
				}

				hLine(poseStack, x, x + width - 1, y + height - 4, 0x33FFFFFF);
			}
		};

		buttonWidget.setTooltip(new TranslatableComponent(pipeline.descriptionKey));

		return buttonWidget;
	}
}
