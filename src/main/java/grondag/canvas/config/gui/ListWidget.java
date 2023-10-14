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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.util.FormattedCharSequence;

public class ListWidget extends ContainerObjectSelectionList<ListItem> {
	public static final int ITEM_HEIGHT = 20;
	public static final int ITEM_SPACING = 2; // multiples of two

	private final int rowWidth;
	private final boolean darkened;

	public ListWidget(int x, int y, int width, int height, boolean darkened) {
		// The workings of these coordinates are arcane by nature
		super(Minecraft.getInstance(), width, height + y, y, height + y, ITEM_HEIGHT + ITEM_SPACING);
		setRenderBackground(false);
		x0 = x;
		x1 = x + width;
		rowWidth = Math.min(300, width - 20);
		this.darkened = darkened;
	}

	public ListWidget(int x, int y, int width, int height) {
		this(x, y, width, height, false);
	}

	@Override
	public void render(GuiGraphics graphics, int i, int j, float f) {
		if (darkened) {
			graphics.fill(x0, y0, x1, y1, 0x99000000);
		}

		super.render(graphics, i, j, f);

		// Render top and bottom shadow over items but not scroll bar
		final boolean hasScrollBar = this.getMaxScroll() > 0;
		final int limit = hasScrollBar ? this.getScrollbarPosition() : this.x1;
		graphics.fillGradient(this.x0, this.y0, limit, this.y0 + 4, -16777216, 0);
		graphics.fillGradient(this.x0, this.y1 - 4, limit, this.y1, 0, -16777216);
	}

	@Override
	public int getRowWidth() {
		return rowWidth;
	}

	@Override
	protected int getScrollbarPosition() {
		return x1 - 5;
	}

	public int addCategory(String key) {
		if (children().size() > 0) {
			addItem(new Category());
		}

		return addItem(new Category(key));
	}

	public int addItem(ListItem item) {
		final int i = addEntry(item);

		item.clearWidgets();
		item.createWidget(getRowLeft(), getRowTop(i) + ITEM_SPACING / 2, getRowWidth(), ITEM_HEIGHT);

		return i;
	}

	public int getY() {
		return y0;
	}

	public int getHeight() {
		return y1 - y0;
	}

	public int getChildScroll(int i) {
		return i * this.itemHeight;
	}

	public List<FormattedCharSequence> getTooltip(int i, int j) {
		if (i < getRowLeft() || i > getRowRight() - 1 || !isMouseOver(i, j)) {
			return null;
		}

		final double relativeY = j - y0 - headerHeight + getScrollAmount();
		final int index = (int) (relativeY / itemHeight);
		final int innerY = (int) relativeY - getChildScroll(index) - ITEM_SPACING / 2 - 1;

		if (innerY < 2 || innerY > ITEM_HEIGHT + 1) {
			// mouse within empty space between list items
			return null;
		}

		if (index >= 0 && index < children().size()) {
			return children().get(index).getTooltip();
		}

		return null;
	}
}
