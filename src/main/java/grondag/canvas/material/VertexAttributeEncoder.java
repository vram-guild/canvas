package grondag.canvas.material;

import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.packing.VertexCollector;

@FunctionalInterface
public interface VertexAttributeEncoder {
    void encode(QuadViewImpl quad, int vertexIndex, VertexEncodingContext context, VertexCollector output);
    
    public static final VertexAttributeEncoder POS = (q, i, c, o) -> {
        if(c.pos == null) {
            o.pos(q.x(i), q.y(i), q.z(i));
        } else {
            o.pos(c.pos, q.x(i), q.y(i), q.z(i));
        }
    };
    
    public static final VertexAttributeEncoder SPRITE_COLOR_0 = (q, i, c, o) -> o.add(q.spriteColor(i, 0));
    
    public static final VertexAttributeEncoder SPRITE_UV_0 = (q, i, c, o) -> {
        o.add(q.spriteU(i, 0));
        o.add(q.spriteV(i, 0));
    };
    
    public static final VertexAttributeEncoder LIGHTMAP = (q, i, c, o) -> {
        int packedLight = q.lightmap(i);
        if(c.context == ShaderContext.ITEM_WORLD) {
            packedLight = ColorHelper.maxBrightness(packedLight, ItemRenderContext.playerLightmap());
        }
        int blockLight = (packedLight & 0xFF);
        int skyLight = ((packedLight >> 16) & 0xFF);
        o.add(blockLight | (skyLight << 8) | (c.mat.shaderFlags() << 16));
    };
    
    public static final VertexAttributeEncoder HD_BLOCK_LIGHTMAP = (q, i, c, o) -> o.add(q.blockLight == null ? 0 : q.blockLight.coord(q, i));
    
    public static final VertexAttributeEncoder HD_SKY_LIGHTMAP = (q, i, c, o) -> o.add(q.skyLight == null ? 0 : q.skyLight.coord(q, i));
    
    public static final VertexAttributeEncoder HD_AO_SHADEMAP = (q, i, c, o) -> o.add(q.aoShade == null ? 0 : q.aoShade.coord(q, i));
    
    public static final VertexAttributeEncoder NORMAL_AO = (q, i, c, o) -> {
        if(c.context.isItem) {
            o.add(q.packedNormal(i) | 0x7F000000);
        } else {
            final float[] aoData = c.aoData;
            int ao = aoData == null ? 0xFF000000 : ((Math.round(aoData[i] * 254) - 127) << 24);
            o.add(q.packedNormal(i) | ao);
        }
    };
    
    public static final VertexAttributeEncoder SPRITE_COLOR_1 = (q, i, c, o) -> {
        if(c.mat.spriteDepth() > 1) {
            o.add(q.spriteColor(i, 1));
        } else {
            o.add(-1);
        }
    };
    
    public static final VertexAttributeEncoder SPRITE_UV_1 = (q, i, c, o) -> {
        if(c.mat.spriteDepth() > 1) {
            o.add(q.spriteU(i, 1));
            o.add(q.spriteV(i, 1));
        } else {
            o.add(0);
            o.add(0);
        }
    };
    
    public static final VertexAttributeEncoder SPRITE_COLOR_2 = (q, i, c, o) -> {
        if(c.mat.spriteDepth() > 2) {
            o.add(q.spriteColor(i, 2));
        } else {
            o.add(-1);
        }
    };
    
    public static final VertexAttributeEncoder SPRITE_UV_2 = (q, i, c, o) -> {
        if(c.mat.spriteDepth() > 2) {
            o.add(q.spriteU(i, 2));
            o.add(q.spriteV(i, 2));
        } else {
            o.add(0);
            o.add(0);
        }
    };
}
