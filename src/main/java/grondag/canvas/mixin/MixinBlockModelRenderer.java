package grondag.canvas.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;

@Mixin(BlockModelRenderer.class)
public abstract class MixinBlockModelRenderer {
	/**
	 * @reason performance; less bad than inject and cancel at head
	 */
	@Overwrite
	public boolean render(BlockRenderView blockView, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrix, VertexConsumer buffer, boolean checkSides, Random rand, long seed, int overlay) {
		return BlockRenderContext.get().tesselate((BlockModelRenderer) (Object) this, blockView, model, state, pos, matrix, buffer, checkSides, seed, overlay);
	}
}
