package grondag.canvas.apiimpl.fluid;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import grondag.canvas.Configurator;
import grondag.frex.api.fluid.AbstractFluidModel;

public class WaterFluidModel extends AbstractFluidModel {
	public WaterFluidModel() {
		super(FluidHandler.WATER_MATERIAL, Configurator.blendFluidColors);
	}

	protected final Sprite[] sprites = FluidHandler.waterSprites();
	protected final Sprite overlaySprite = ModelLoader.WATER_OVERLAY.getSprite();

	@Override
	public int getFluidColor(BlockRenderView view, BlockPos pos, FluidState state) {
		return BiomeColors.getWaterColor(view, pos);
	}

	@Override
	public Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state) {
		return sprites;
	}

	@Override
	protected boolean needsOverlay() {
		return true;
	}

	@Override
	protected Sprite overlaySprite() {
		return overlaySprite;
	}
}
