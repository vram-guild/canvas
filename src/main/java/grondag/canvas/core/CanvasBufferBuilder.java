package grondag.canvas.core;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;

public class CanvasBufferBuilder extends BufferBuilder {
    public CanvasBufferBuilder(int size) {
        super(size);
    }

    VertexCollectorList vcList = new VertexCollectorList();

    @Override
    public void setOffset(double x, double y, double z) {
        vcList.setRenderOrigin(-x, -y, -z);
        super.setOffset(x, y, z);
    }
    
    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void begin(int primitive, VertexFormat format) {
        super.begin(primitive, format);
    }

    @Override
    public void end() {
        super.end();
    }
}
