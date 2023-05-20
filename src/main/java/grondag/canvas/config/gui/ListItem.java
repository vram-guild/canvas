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

package grondag.canvas.config.gui;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.util.FormattedCharSequence;

import grondag.canvas.config.ConfigManager;

public abstract class ListItem extends ContainerObjectSelectionList.Entry<ListItem> {
	private final ObjectArrayList<AbstractWidget> children;
	protected final String key;
	private final List<FormattedCharSequence> tooltip;

	@SuppressWarnings("resource")
	protected ListItem(String key, @Nullable String tooltipKey) {
		this.key = key;
		children = new ObjectArrayList<>(2);

		if (tooltipKey == null) {
			this.tooltip = null;
		} else {
			this.tooltip = Minecraft.getInstance().font.split(ConfigManager.parseTooltip(tooltipKey), 200);
		}
	}

	protected void clearWidgets() {
		children.clear();
	}

	protected abstract void createWidget(int x, int y, int width, int height);

	protected void add(AbstractWidget widget) {
		children.add(widget);
	}

	@Override
	public final List<? extends NarratableEntry> narratables() {
		return children;
	}

	@Override
	public final void render(@NotNull GuiGraphics graphics, int i, int scrollY, int left, int l, int m, int mouseX, int mouseY, boolean bl, float f) {
		this.children.forEach((child) -> {
			child.setY(scrollY);
			child.render(graphics, mouseX, mouseY, f);
		});
	}

	@Override
	public final List<? extends GuiEventListener> children() {
		return children;
	}

	public List<FormattedCharSequence> getTooltip() {
		return tooltip;
	}
}
