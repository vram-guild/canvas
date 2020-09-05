package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.render.RenderLayer;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.mixinterface.RenderLayerExt;

@Mixin(RenderLayer.class)
public class MixinRenderLayer implements RenderLayerExt {
	@Shadow private boolean translucent;

	private int blendModeIndex = BlendMode.DEFAULT.ordinal();

	@Override
	public boolean canvas_isTranslucent() {
		return translucent;
	}

	@Override
	public void canvas_blendModeIndex(int blendModeIndex) {
		this.blendModeIndex = blendModeIndex;
	}

	@Override
	public int canvas_blendModeIndex() {
		return blendModeIndex;
	}
}
