package grondag.canvas.apiimpl.fluid;

import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;

import grondag.canvas.Configurator;
import grondag.frex.api.fluid.AbstractFluidModel;

public class FluidHandlerModel extends AbstractFluidModel {
	protected final FluidRenderHandler handler;
	protected final Sprite overlaySprite = ModelLoader.WATER_OVERLAY.getSprite();

	public FluidHandlerModel(Fluid fluid, FluidRenderHandler handler) {
		super(fluid  == Fluids.FLOWING_LAVA || fluid == Fluids.LAVA ? FluidHandler.LAVA_MATERIAL : FluidHandler.WATER_MATERIAL, Configurator.blendFluidColors);
		this.handler = handler;
	}

	@Override
	public int getFluidColor(BlockRenderView view, BlockPos pos, FluidState state) {
		return handler.getFluidColor(view, pos, state);
	}

	@Override
	public Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state) {
		return handler.getFluidSprites(view, pos, state);
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
