package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Function;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.encoding.VertexEncodingContext;
import grondag.canvas.buffer.packing.VertexCollectorList;

public abstract class AbstractBlockEncodingContext extends VertexEncodingContext {
	protected final BlockRenderInfo blockInfo;

	public AbstractBlockEncodingContext(BlockRenderInfo blockInfo, Function<RenderLayer, VertexConsumer> bufferFunc, VertexCollectorList collectors) {
		super(bufferFunc, collectors);
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
