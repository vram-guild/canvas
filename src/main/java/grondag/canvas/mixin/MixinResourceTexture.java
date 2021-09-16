package grondag.canvas.mixin;

import grondag.canvas.varia.GFX;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceTexture.class)
public abstract class MixinResourceTexture extends AbstractTexture {
	@Shadow
	@Final
	protected Identifier location;

	@Inject(method = "upload", at = @At("TAIL"))
	private void onUpload(NativeImage nativeImage, boolean bl, boolean bl2, CallbackInfo ci) {
		GFX.objectLabel(GL11.GL_TEXTURE, getGlId(), "IMG " + location.toString());
	}
}
