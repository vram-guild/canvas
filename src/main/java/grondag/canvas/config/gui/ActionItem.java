package grondag.canvas.config.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class ActionItem extends ListItem {
	private final ButtonFactory factory;
	private final Runnable action;
	private Button button;

	public ActionItem(String key, String tooltipKey, ButtonFactory factory, Runnable action) {
		super(key, tooltipKey);
		this.factory = factory;
		this.action = action;
	}

	public ActionItem(String key, ButtonFactory factory, Runnable action) {
		this(key, null, factory, action);
	}

	@Override
	protected void createWidget(int x, int y, int width, int height) {
		button = factory.createButton(x, y, width, height, new TranslatableComponent(key), b -> action.run());

		add(button);
	}

	public interface ButtonFactory {
		Button createButton(int x, int y, int width, int height, Component component, Button.OnPress action);
	}
}
