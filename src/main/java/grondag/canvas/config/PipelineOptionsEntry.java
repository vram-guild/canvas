package grondag.canvas.config;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.AbstractPressableButtonWidget;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

public class PipelineOptionsEntry extends TooltipListEntry<Void> {
	private final AbstractButtonWidget buttonWidget = new AbstractPressableButtonWidget(0, 0, 115, 20, NarratorManager.EMPTY) {
		@Override
		public void onPress() {
			MinecraftClient.getInstance().openScreen(PipelineOptionGui.display(ConfigGui.pipeline()));
		}

		@Override
		public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
			setMessage(new TranslatableText("config.canvas.value.pipeline_config"));
			super.render(matrices, mouseX, mouseY, delta);
		}
	};

	private final List<Element> children = ImmutableList.of(buttonWidget);

	@SuppressWarnings("deprecation")
	public PipelineOptionsEntry() {
		super(new TranslatableText("config.canvas.value.pipeline_config"), () -> Optional.of(ConfigManager.parse("config.canvas.help.pipeline_config")), false);
	}

	@Override
	public List<? extends Element> children() {
		return children;
	}

	@Override
	public Void getValue() {
		return null;
	}

	@Override
	public Optional<Void> getDefaultValue() {
		return null;
	}

	@Override
	public void save() {
		// NOOP
	}

	@SuppressWarnings("resource")
	@Override
	public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
		super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
		buttonWidget.y = y;

		if (MinecraftClient.getInstance().textRenderer.isRightToLeft()) {
			buttonWidget.x = x + 150 - buttonWidget.getWidth();
		} else {
			buttonWidget.x = x + entryWidth - 150;
		}

		buttonWidget.render(matrices, mouseX, mouseY, delta);
	}
}
