package grondag.canvas.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.texture.SpriteContents;

import grondag.canvas.mixinterface.AnimatedTextureExt;
import grondag.canvas.mixinterface.AnimatedTextureTickerExt;
import grondag.canvas.mixinterface.SpriteContentsExt;

@Mixin(SpriteContents.Ticker.class)
public class MixinAnimatedTextureTicker implements AnimatedTextureTickerExt {
	@Shadow(aliases = {"this$0", "a", "field_40543"})
	@Dynamic
	private SpriteContents parent;
	@Shadow
	int frame;
	@Shadow int subFrame;

	@Nullable
	@Shadow @Final
	private SpriteContents.InterpolationData interpolationData;

	@Shadow @Final private SpriteContents.AnimatedTexture animationInfo;

	@Override
	public int canvas_frameIndex() {
		return frame;
	}

	@Override
	public int canvas_frameTicks() {
		return subFrame;
	}

	@Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
	private void beforeTick(CallbackInfo ci) {
		if (!((SpriteContentsExt) parent).canvas_shouldAnimate()) {
			ci.cancel();
		}
	}

	@Override
	public SpriteContents.InterpolationData canvas_interpolation() {
		return interpolationData;
	}

	@Override
	public int canvas_frameCount() {
		return ((AnimatedTextureExt) animationInfo).canvas_frameCount();
	}

	@Override
	public List<SpriteContents.FrameInfo> canvas_frames() {
		return ((AnimatedTextureExt) animationInfo).canvas_frames();
	}
}
