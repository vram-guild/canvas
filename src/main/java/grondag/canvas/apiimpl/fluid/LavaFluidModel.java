package grondag.canvas.apiimpl.fluid;

import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import grondag.frex.api.fluid.AbstractFluidModel;

public class LavaFluidModel extends AbstractFluidModel {
	public LavaFluidModel() {
		super(FluidHandler.LAVA_MATERIAL, false);
	}

	protected final Sprite[] sprites = FluidHandler.lavaSprites();

	@Override
	public int getFluidColor(BlockRenderView view, BlockPos pos, FluidState state) {
		return -1;
	}

	@Override
	public Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state) {
		return sprites;
	}
}
