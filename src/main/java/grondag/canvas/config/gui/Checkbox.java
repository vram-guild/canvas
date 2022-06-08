package grondag.canvas.config.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class Checkbox extends Button {
	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
	private boolean value;
	private final Runnable onValueChange;

	public Checkbox(int x, int y, int width, int height, Component component, Runnable onValueChange) {
		super(x, y, width, height, component, ON_PRESS);
		this.onValueChange = onValueChange;
	}

	public boolean getValue() {
		return value;
	}

	public void changeValueSilently(boolean value) {
		this.value = value;
	}

	@Override
	public void renderButton(PoseStack poseStack, int i, int j, float f) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderSystem.setShaderTexture(0, TEXTURE);
		RenderSystem.enableDepthTest();
		Font font = minecraft.font;
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		blit(poseStack, this.x, this.y, this.isFocused() ? 20.0F : 0.0F, this.value ? 20.0F : 0.0F, 20, this.height, 64, 64);
		this.renderBg(poseStack, minecraft, i, j);
		drawString(poseStack, font, this.getMessage(), this.x + 24, this.y + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
	}

	private void toggleValue() {
		value = !value;
		onValueChange.run();
	}

	private static final OnPress ON_PRESS = button -> {
		if (button instanceof Checkbox checkbox) {
			checkbox.toggleValue();
		}
	};
}
