package grondag.canvas.mixinterface;

import java.util.Map;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

public interface SpriteAtlasTextureExt {
	Map<Identifier, Sprite> canvas_sprites();
}
