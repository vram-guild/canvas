package grondag.canvas.core;

import java.nio.ByteBuffer;

import grondag.canvas.RendererImpl;
import grondag.canvas.helper.ColorHelper;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.BlockPos;

public class FluidBufferBuilder extends BufferBuilder {
    private static final int DEFAULT_SHADER_FLAGS = RendererImpl.INSTANCE.materialFinder()
            .disableDiffuse(0, true).disableAo(0, true).find().shaderFlags() << 16;
    public FluidBufferBuilder() {
        super(256);
    }

    VertexCollector vc;
    BlockPos pos;
    
    public FluidBufferBuilder prepare(VertexCollector vc, BlockPos pos, BlockRenderLayer layer) {
        this.vc = vc;
        this.pos = pos;
        return this;
    }
    
    
    @Override
    public BufferBuilder vertex(double x, double y, double z) {
        //fluid renderer will already include block pos offset so unfortunately we need to remove it
        vc.pos(pos, (float)(x - pos.getX()), (float)(y - pos.getY()), (float)(z - pos.getZ()));
        return this;
    }

    @Override
    public BufferBuilder color(float r, float g, float b, float a) {
        int color = (int)(b * 255f);
        color |= (int)(g * 255f) << 8;
        color |= (int)(r * 255f) << 16;
        color |= (int)(a * 255f) << 24;
        vc.add(ColorHelper.swapRedBlueIfNeeded(color));
        return this;
    }

    @Override
    public BufferBuilder texture(double u, double v) {
        vc.add((float)u);
        vc.add((float)v);
        return this;
    }

    @Override
    public BufferBuilder texture(int skyLight, int blockLight) {
        vc.add((blockLight & 0xFF) | ((skyLight & 0xFF) << 8) | DEFAULT_SHADER_FLAGS);
        return this;
    }

    @Override
    public void next() {
        // FIXME compute legit normal - for now is padding
        // FIXME move Ao to shader - for now is disabled and happens CPU-side
        vc.add(0);
    }

    @Override
    public void sortQuads(float float_1, float float_2, float float_3) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public State toBufferState() {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void restoreState(State bufferBuilder$State_1) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void begin(int int_1, VertexFormat vertexFormat_1) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void brightness(int int_1, int int_2, int int_3, int int_4) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void postPosition(double double_1, double double_2, double double_3) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void multiplyColor(float float_1, float float_2, float float_3, int int_1) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void setColor(float float_1, float float_2, float float_3, int int_1) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void disableColor() {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public BufferBuilder color(int int_1, int int_2, int int_3, int int_4) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void putVertexData(int[] ints_1) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void postNormal(float float_1, float float_2, float float_3) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public BufferBuilder normal(float float_1, float float_2, float float_3) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void setOffset(double double_1, double double_2, double double_3) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void end() {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public ByteBuffer getByteBuffer() {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public VertexFormat getVertexFormat() {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public int getVertexCount() {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public int getDrawMode() {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void setQuadColor(int int_1) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }

    @Override
    public void setQuadColor(float float_1, float float_2, float float_3) {
        throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
    }
    
    
}
