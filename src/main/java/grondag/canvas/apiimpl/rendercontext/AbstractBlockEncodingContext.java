package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Function;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

public abstract class AbstractBlockEncodingContext extends AbstractEncodingContext {
	protected final BlockRenderInfo blockInfo;

	public AbstractBlockEncodingContext(BlockRenderInfo blockInfo, Function<RenderLayer, VertexConsumer> bufferFunc, QuadTransform transform) {
		super(bufferFunc, transform, blockInfo::shouldDrawFace);
		this.blockInfo = blockInfo;
	}

	@Override
	public VertexConsumer consumer(MutableQuadViewImpl quad) {
		final RenderLayer layer = blockInfo.effectiveRenderLayer(quad.material().blendMode(0));
		return bufferFunc.apply(layer);
	}

	@Override
	public final int indexedColor(int colorIndex) {
		return blockInfo.blockColor(colorIndex);
	}

	@Override
	public final void applyLighting(MutableQuadViewImpl quad) {
		blockInfo.applyBlockLighting(quad);
	}
}
