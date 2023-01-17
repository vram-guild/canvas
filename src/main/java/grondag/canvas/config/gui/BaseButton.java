package grondag.canvas.config.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class BaseButton extends Button {
	public BaseButton(int i, int j, int k, int l, Component component, OnPress onPress) {
		super(i, j, k, l, component, onPress, DEFAULT_NARRATION);
	}
}
