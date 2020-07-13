package grondag.canvas.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

import grondag.canvas.mixinterface.SpriteAtlasTextureExt;

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTexture implements SpriteAtlasTextureExt {
	@Shadow
	private Map<Identifier, Sprite> sprites;

	@Override
	public Map<Identifier, Sprite> canvas_sprites() {
		return sprites;
	}
}
