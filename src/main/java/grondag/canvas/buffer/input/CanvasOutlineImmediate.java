package grondag.canvas.buffer.input;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource.EntityOutlineGenerator;
import net.minecraft.client.renderer.RenderType;

// This class can't extend CanvasImmediate because item renderer treats it as special case
public class CanvasOutlineImmediate implements MultiBufferSource {
	private final CanvasImmediate mainImmediate;
	private final CanvasImmediate auxImmediate;

	private int outlineRed = 255;
	private int outlineGreen = 255;
	private int outlineBlue = 255;
	private int outlineAlpha = 255;

	public CanvasOutlineImmediate(CanvasImmediate mainImmediate) {
		this.mainImmediate = mainImmediate;
		this.auxImmediate = new CanvasImmediate(new BufferBuilder(256), ImmutableMap.of(), mainImmediate.contextState);
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		if (renderType.isOutline()) {
			final VertexConsumer auxConsumer = auxImmediate.getBuffer(renderType);
			return new EntityOutlineGenerator(auxConsumer, outlineRed, outlineGreen, outlineBlue, outlineAlpha);
		} else {
			Optional<RenderType> optionalOutline = renderType.outline();

			if (optionalOutline.isPresent()) {
				final VertexConsumer mainConsumer = this.mainImmediate.getBuffer(renderType);
				final VertexConsumer auxConsumer = auxImmediate.getBuffer(optionalOutline.get());
				final EntityOutlineGenerator entityOutlineGenerator = new EntityOutlineGenerator(auxConsumer, outlineRed, outlineGreen, outlineBlue, 255);
				return new FixedDouble(entityOutlineGenerator, mainConsumer);
			} else {
				return this.mainImmediate.getBuffer(renderType);
			}
		}
	}

	public void setColor(int red, int green, int blue, int alpha) {
		outlineRed = red;
		outlineGreen = green;
		outlineBlue = blue;
		outlineAlpha = alpha;
	}

	public void endOutlineBatch() {
		auxImmediate.endBatch();
	}

	private static class FixedDouble extends VertexMultiConsumer.Double {
		private final VertexConsumer a, b;

		FixedDouble(VertexConsumer vertexConsumer, VertexConsumer vertexConsumer2) {
			super(vertexConsumer, vertexConsumer2);
			a = vertexConsumer;
			b = vertexConsumer2;
		}

		// This fixes lighting on items that have outline. Don't ask me why
		@Override
		public VertexConsumer uv2(int i) {
			a.uv2(i);
			b.uv2(i);
			return this;
		}
	}
}
