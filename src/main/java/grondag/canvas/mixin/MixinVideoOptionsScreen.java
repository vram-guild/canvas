package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import grondag.canvas.varia.CanvasButtonWidget;

@Mixin(VideoOptionsScreen.class)
public class MixinVideoOptionsScreen extends Screen {
	public MixinVideoOptionsScreen(Text title) {
		super(title);
	}

	@Inject(at = @At("RETURN"), method = "init()V")
	public void drawMenuButton(CallbackInfo info) {
		addButton(new CanvasButtonWidget(width - 100, 5, 90, 20, new TranslatableText("config.canvas.button"), this));
	}
}
