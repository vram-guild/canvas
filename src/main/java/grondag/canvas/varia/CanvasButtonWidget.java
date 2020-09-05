package grondag.canvas.varia;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import grondag.canvas.Configurator;

public class CanvasButtonWidget extends ButtonWidget {
	public CanvasButtonWidget(int x, int y, int width, int height, Text text, Screen screen) {
		super(x, y, width, height, text, button -> MinecraftClient.getInstance().openScreen(Configurator.display(screen)));
	}
}
