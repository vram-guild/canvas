package grondag.canvas.mixin;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

import grondag.canvas.mixinterface.SpriteAtlasTextureExt;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.texture.SpriteInfoTexture;

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTexture implements SpriteAtlasTextureExt {
	@Shadow private Identifier id;
	@Shadow private Map<Identifier, Sprite> sprites;

	private final ObjectArrayList<Sprite> spriteIndex = new ObjectArrayList<>();

	@Override
	public Map<Identifier, Sprite> canvas_spriteMap() {
		return sprites;
	}

	@Override
	public ObjectArrayList<Sprite> canvas_spriteIndex() {
		return spriteIndex;
	}

	@Inject(at = @At("RETURN"), method = "upload")
	private void afterUpload(SpriteAtlasTexture.Data input, CallbackInfo info) {
		if (id.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEX)) {
			SpriteInfoTexture.reset(input);
			int index = 0;

			for (final Sprite sprite : sprites.values()) {
				spriteIndex.add(sprite);
				((SpriteExt) sprite).canvas_id(index++);
			}
		}
	}
}
