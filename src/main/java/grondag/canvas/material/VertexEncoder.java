package grondag.canvas.material;

import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.packing.VertexCollector;
import net.minecraft.util.math.BlockPos;

public class VertexEncoder {
    private static final MaterialVertexFormat[] FORMATS = MaterialVertexFormat.values();
    
    public static MaterialVertexFormat format(int shaderProps) {
        return FORMATS[ShaderProps.spriteDepth(shaderProps) - 1];
    }
    
    public static void encodeBlock(QuadViewImpl q, RenderMaterialImpl.Value mat, ShaderContext context, VertexCollector output, BlockPos pos, float[] aoData) {
        final int shaderFlags = mat.shaderFlags() << 16;
        final int shaderProps = output.materialState().shaderProps;
        final int depth = mat.spriteDepth();
        assert depth == ShaderProps.spriteDepth(shaderProps);
        
        for(int i = 0; i < 4; i++) {
            output.pos(pos, q.x(i), q.y(i), q.z(i));
            output.add(q.spriteColor(i, 0));
            output.add(q.spriteU(i, 0));
            output.add(q.spriteV(i, 0));
            int packedLight = q.lightmap(i);
            int blockLight = (packedLight & 0xFF);
            int skyLight = ((packedLight >> 16) & 0xFF);
            output.add(blockLight | (skyLight << 8) | shaderFlags);
            int ao = aoData == null ? 0xFF000000 : ((Math.round(aoData[i] * 254) - 127) << 24);
            output.add(q.packedNormal(i) | ao);
            
            if(depth > 1) {
                output.add(q.spriteColor(i, 1));
                output.add(q.spriteU(i, 1));
                output.add(q.spriteV(i, 1));
                
                if(depth == 3) {
                    output.add(q.spriteColor(i, 2));
                    output.add(q.spriteU(i, 2));
                    output.add(q.spriteV(i, 2));
                }
            }
        }
    }
    
    
    // material because varies from what is in quad in rare cases (enchantment glint)
    public static void encodeItem(QuadViewImpl quad, RenderMaterialImpl.Value mat, ShaderContext context, VertexCollector output) {
        final int shaderFlags = mat.shaderFlags() << 16;
        final int shaderProps = output.materialState().shaderProps;
        final int depth = mat.spriteDepth();
        assert depth == ShaderProps.spriteDepth(shaderProps);
        
        for(int i = 0; i < 4; i++) {
            output.pos(quad.x(i), quad.y(i), quad.z(i));
            output.add(quad.spriteColor(i, 0));
            output.add(quad.spriteU(i, 0));
            output.add(quad.spriteV(i, 0));
            int packedLight = quad.lightmap(i);
            if(context == ShaderContext.ITEM_WORLD) {
                packedLight = ColorHelper.maxBrightness(packedLight, ItemRenderContext.playerLightmap());
            }
            int blockLight = (packedLight & 0xFF);
            int skyLight = ((packedLight >> 16) & 0xFF);
            output.add(blockLight | (skyLight << 8) | shaderFlags);
            output.add(quad.packedNormal(i) | 0x7F000000);
            
            if(depth > 1) {
                output.add(quad.spriteColor(i, 1));
                output.add(quad.spriteU(i, 1));
                output.add(quad.spriteV(i, 1));
                
                if(depth == 3) {
                    output.add(quad.spriteColor(i, 2));
                    output.add(quad.spriteU(i, 2));
                    output.add(quad.spriteV(i, 2));
                }
            }
        }
    }
}
