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
		super(Minecraft.getInstance(), width, height, y, ITEM_HEIGHT + ITEM_SPACING);
		setRenderBackground(false);
		setX(x);
		rowWidth = Math.min(300, width - 20);
		this.darkened = darkened;
	}

	public ListWidget(int x, int y, int width, int height) {
		this(x, y, width, height, false);
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int i, int j, float f) {
		graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), darkened ? 0x99000000 : 0x33000000);

		super.renderWidget(graphics, i, j, f);

		// Render top and bottom shadow over items but not scroll bar
		final int limit = this.getMaxScroll() > 0 ? this.getScrollbarPosition() : this.getX() + getWidth();
		graphics.fillGradient(getX(), this.getY(), limit, this.getY() + 4, -16777216, 0);
		graphics.fillGradient(getX(), this.getY() + getHeight() - 4, limit, this.getY() + getHeight(), 0, -16777216);
	}

	@Override
	public int getRowWidth() {
		return rowWidth;
	}

	@Override
	protected int getScrollbarPosition() {
		return getX() + getWidth() - SCROLLBAR_WIDTH;
	}

	public int addCategory(String key) {
		if (!children().isEmpty()) {
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

	public int getChildScroll(int i) {
		return i * this.itemHeight;
	}

	public List<FormattedCharSequence> getTooltip(int i, int j) {
		if (i < getRowLeft() || i > getRowRight() - 1 || !isMouseOver(i, j)) {
			return null;
		}

		final double relativeY = j - getY() - headerHeight + getScrollAmount();
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
