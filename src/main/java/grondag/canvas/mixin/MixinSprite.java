package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.texture.Sprite;

import grondag.canvas.mixinterface.SpriteExt;

@Mixin(Sprite.class)
public class MixinSprite implements SpriteExt {
	private int canvasId;

	@Override
	public int canvas_id() {
		return canvasId;
	}

	@Override
	public void canvas_id(int id) {
		canvasId = id;
	}
}
