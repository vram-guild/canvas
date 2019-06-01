package grondag.canvas.material;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import net.minecraft.util.math.BlockPos;

public final class VertexEncodingContext {
    RenderMaterialImpl.Value mat;
    ShaderContext context;
    BlockPos pos;
    float[] aoData;
    
    public VertexEncodingContext prepare(RenderMaterialImpl.Value mat, ShaderContext context, BlockPos pos, float[] aoData) {
        this.mat = mat;
        this.context = context;
        this.pos = pos;
        this.aoData = aoData;
        return this;
    }
}
