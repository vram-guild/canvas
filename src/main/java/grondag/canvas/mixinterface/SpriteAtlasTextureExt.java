package grondag.canvas.mixinterface;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

public interface SpriteAtlasTextureExt {
	Map<Identifier, Sprite> canvas_spriteMap();

	ObjectArrayList<Sprite> canvas_spriteIndex();
}
