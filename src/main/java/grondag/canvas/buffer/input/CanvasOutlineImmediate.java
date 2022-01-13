package grondag.canvas.buffer.input;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;

import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;

public class CanvasOutlineImmediate extends CanvasImmediate{
    private final CanvasImmediate mainImmediate;

    private int outlineRed = 255;
    private int outlineGreen = 255;
    private int outlineBlue = 255;
    private int outlineAlpha = 255;

    public CanvasOutlineImmediate(CanvasImmediate mainImmediate) {
        super(new BufferBuilder(256), ImmutableMap.of(), mainImmediate.contextState);
        this.mainImmediate = mainImmediate;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        VertexConsumer auxConsumer;
        if (renderType.isOutline()) {
            auxConsumer = super.getBuffer(renderType);
            return new OutlineBufferSource.EntityOutlineGenerator(auxConsumer, outlineRed, outlineGreen, outlineBlue, outlineAlpha);
        } else {
            Optional<RenderType> optionalOutline = renderType.outline();
            if (optionalOutline.isPresent()) {
                VertexConsumer mainConsumer = this.mainImmediate.getBuffer(renderType);
                auxConsumer = super.getBuffer(optionalOutline.get());
                OutlineBufferSource.EntityOutlineGenerator entityOutlineGenerator = new OutlineBufferSource.EntityOutlineGenerator(auxConsumer, outlineRed, outlineGreen, outlineBlue, 255);
                return VertexMultiConsumer.create(entityOutlineGenerator, mainConsumer);
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
}
