package grondag.canvas.config;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.EmptyBackground;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.option.SpruceSimpleActionOption;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.config.widget.Buttons;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineConfigBuilder;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.pipeline.config.option.OptionConfig;

public class PipelineOptionScreen extends SpruceScreen {
	private static final TranslatableComponent EMPTY_TEXT = new TranslatableComponent("config.canvas.category.empty");

	private final ResourceLocation pipelineId;
	private final String pipelineName;
	private final boolean isEmpty;
	private final OptionConfig[] configs;
	private final Screen parent;

	public PipelineOptionScreen(Screen parent, ResourceLocation pipelineId) {
		super(new TranslatableComponent("config.canvas.value.pipeline_config"));
		this.parent = parent;
		this.pipelineId = pipelineId;

		final PipelineConfig config = PipelineConfigBuilder.build(pipelineId);
		this.pipelineName = I18n.get(PipelineLoader.get(pipelineId.toString()).nameKey);
		this.configs = config.options;
		ConfigManager.initPipelineOptions(configs);

		this.isEmpty = configs.length == 0;
	}

	public void switchBack(ResourceLocation newPipelineId) {
		if (newPipelineId.equals(pipelineId)) {
			minecraft.setScreen(this);
		} else {
			savePipelineSelection(newPipelineId);

			minecraft.setScreen(new PipelineOptionScreen(this.parent, newPipelineId));
		}
	}

	@Override
	protected void init() {
		super.init();

		Buttons.sideW = (this.width - 330) >= 72 ? Math.min(120, this.width - 330) : 0;
		final int rightSideW = Math.min(Buttons.sideW, Math.max(0, this.width - 330 - Buttons.sideW));

		final SpruceOptionListWidget list = new SpruceOptionListWidget(Position.of(Buttons.sideW + 2, 22 + 2), this.width - Buttons.sideW - 4 - rightSideW, this.height - 35 - 22 - 2);
		list.setBackground(new SimpleColorBackground(0xAA000000));
		addWidget(list);

		list.addSingleOptionEntry(new SpruceSimpleActionOption(pipelineName,
			(p, w, m, a) -> Buttons.browseButton(p, w, new TextComponent(I18n.get("config.canvas.value.pipeline") + ": " + pipelineName), a),
			b -> minecraft.setScreen(new PipelineSelectionScreen(this))));

		if (configs.length > 0) {
			final SpruceOptionListWidget tabs = new SpruceOptionListWidget(Position.of(1, list.getY()), Buttons.sideW, list.getHeight());
			tabs.setBackground(EmptyBackground.EMPTY_BACKGROUND);
			addWidget(tabs);

			boolean top = true;

			for (final OptionConfig cfg : configs) {
				final int index = cfg.addGuiEntries(list);
				final int categoryY = top ? 0 : list.children().get(index).getY() - list.getY() - 2;
				top = false;
				tabs.addSingleOptionEntry(new SpruceSimpleActionOption(cfg.categoryKey, Buttons::sideButton, e -> list.setScrollAmount(categoryY)));
			}
		}

		// TO-DO Translatable
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 - 120 - 1, this.height - 35 + 6), 120 - 2, 20, new TextComponent("Save & Quit"), b -> save()));
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 + 1, this.height - 35 + 6), 120 - 2, 20, new TextComponent("Cancel"), b -> close()));
	}

	private void savePipelineSelection(ResourceLocation newPipelineId) {
		Configurator.pipelineId = newPipelineId.toString();
		Configurator.reload = true;
		ConfigManager.saveConfig();
	}

	private void save() {
		ConfigManager.savePipelineOptions(configs);
		close();
	}

	private void close() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void renderTitle(PoseStack matrices, int mouseX, int mouseY, float delta) {
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 8, 16777215);

		if (isEmpty) {
			drawCenteredString(matrices, this.font, EMPTY_TEXT, this.width / 2, this.height / 2 - this.font.lineHeight / 2, 16777215);
		}
	}
}
