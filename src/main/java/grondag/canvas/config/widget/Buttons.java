package grondag.canvas.config.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import net.minecraft.network.chat.Component;

public class Buttons {
	public static int sideW = 0;

	public static SpruceButtonWidget sideButton(Position p, int w, Component m, SpruceButtonWidget.PressAction a) {
		return new SidebarButton(Position.of(p, w / 2 - (Buttons.sideW - 16) / 2, 0), Buttons.sideW - 16, 20, m, a);
	}

	public static SpruceButtonWidget browseButton(Position p, int w, Component m, SpruceButtonWidget.PressAction a) {
		return new BrowseButton(p, w, 20, m, a);
	}

	public static class SidebarButton extends SpruceButtonWidget {
		public SidebarButton(Position position, int width, int height, Component message, PressAction action) {
			super(position, width, height, message, action);
		}

		@Override
		public void renderBackground(PoseStack poseStack, int i, int j, float f) {
			final int x = getX();
			final int y = getY();

			if (isFocusedOrHovered()) {
				fill(poseStack, x, y, x + width, y + height - 3, 0x66FFFFFF);
			}

			hLine(poseStack, x, x + width - 1, y + height - 4, 0x99FFFFFF);
		}
	}

	public static class MinimalistButton extends SpruceButtonWidget {
		public MinimalistButton(Position position, int width, int height, Component message, PressAction action) {
			super(position, width, height, message, action);
		}

		@Override
		public void renderBackground(PoseStack poseStack, int i, int j, float f) {
			final int x = getX();
			final int y = getY();

			if (isFocusedOrHovered()) {
				fill(poseStack, x, y, x + width, y + height, 0x66FFFFFF);
			}

			hLine(poseStack, x, x + width - 1, y, 0x99FFFFFF);
			hLine(poseStack, x, x + width - 1, y + height - 1, 0x99FFFFFF);
			vLine(poseStack, x, y, y + height - 1, 0x99FFFFFF);
			vLine(poseStack, x + width - 1, y, y + height - 1, 0x99FFFFFF);
		}
	}

	public static class BrowseButton extends SpruceButtonWidget {
		public BrowseButton(Position position, int width, int height, Component message, PressAction action) {
			super(position, width, height, message, action);
		}

		@Override
		public void renderBackground(PoseStack ps, int ii, int j, float f) {
			super.renderBackground(ps, ii, j, f);
			final int x = getX();
			final int y = getY();
			final int boxW = getHeight();
			final int box = getHeight() / 2;

//			fill(ps, x + width - boxW / 2 - box / 2, y + boxW / 2 - box / 2, x + width - boxW / 2 + box / 2, y + height - boxW / 2 + box / 2, 0xFFFFFFFF);

			for (int i = 0; i < box / 2; i++) {
				drawArrowShadow(ps, i, x, y, boxW, box);
				drawArrow(ps, i, x, y, boxW, box);
			}
		}

		private void drawArrow(PoseStack ps, int i, int x, int y, int boxW, int box) {
			vLine(ps, x + width - boxW / 2 - box / 2 + i, y + boxW / 2 - box / 2 + i - 1, y + height - boxW / 2 + box / 2 - i - 2, 0xFFFFFFFF);
		}

		private void drawArrowShadow(PoseStack ps, int i, int x, int y, int boxW, int box) {
			vLine(ps, x + width - boxW / 2 - box / 2 + i, y + height - boxW / 2 + box / 2 - i - 3, y + height - boxW / 2 + box / 2 - i - 1, 0x99000000);
		}
	}
}
