package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.texture.SpriteAtlasTexture;

import grondag.canvas.mixinterface.SpriteAtlasTextureDataExt;

@Mixin(SpriteAtlasTexture.Data.class)
public class MixinSpriteAtlasTextureData implements SpriteAtlasTextureDataExt {
	@Shadow int width;
	@Shadow int height;

	@Override
	public int canvas_atlasWidth() {
		return width;
	}

	@Override
	public int canvas_atlasHeight() {
		return height;
	}
}
