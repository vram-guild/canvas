package grondag.canvas.material;

import static grondag.canvas.material.MaterialVertextFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.BASE_TEX_2F;
import static grondag.canvas.material.MaterialVertextFormatElement.HD_AO_SHADEMAP_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.HD_BLOCK_LIGHTMAP_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.HD_SKY_LIGHTMAP_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.NORMAL_AO_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.POSITION_3F;
import static grondag.canvas.material.MaterialVertextFormatElement.SECONDARY_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.SECONDARY_TEX_2F;
import static grondag.canvas.material.MaterialVertextFormatElement.TERTIARY_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.TERTIARY_TEX_2F;

import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.packing.VertexCollector;
import grondag.canvas.light.LightmapHd;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;

public class VertexEncoder {
    private static final Int2ObjectOpenHashMap<MaterialVertexFormat> formats = new Int2ObjectOpenHashMap<>();

    // Note that all logic for what is in or out is in ShaderProps
    // so that if compact is disabled we'll never see those options here
    // This keeps the key space compact.
    
    public static MaterialVertexFormat format(int shaderProps) {
        MaterialVertexFormat result = formats.get(shaderProps);
        if(result == null) {
            synchronized(formats) {
                result = formats.get(shaderProps);
                if(result == null) {
                    result = buildFormat(shaderProps);
                    formats.put(shaderProps, result);
                }
            }
        }
        return result;
    }
    
    public static void forceReload() {
        synchronized(formats) {
            formats.clear();
        }
    }
    
    private static MaterialVertexFormat buildFormat(int shaderProps) {
        final int spriteDepth = ShaderProps.spriteDepth(shaderProps);
        
        ObjectArrayList<MaterialVertextFormatElement> elements = new ObjectArrayList<>();
        elements.add(POSITION_3F);
        
        if((shaderProps & ShaderProps.WHITE_0) == 0) {
            elements.add(BASE_RGBA_4UB);
        }
        
        elements.add(BASE_TEX_2F);
        elements.add(LIGHTMAPS_4UB);
        
        if((shaderProps & ShaderProps.SMOOTH_LIGHTMAPS) == ShaderProps.SMOOTH_LIGHTMAPS) {
            elements.add(HD_BLOCK_LIGHTMAP_2US);
            elements.add(HD_SKY_LIGHTMAP_2US);
            elements.add(HD_AO_SHADEMAP_2US);
        }
        
        elements.add(NORMAL_AO_4UB);
        
        if(spriteDepth > 1) {
            elements.add(SECONDARY_RGBA_4UB);
            elements.add(SECONDARY_TEX_2F);
            if(spriteDepth == 3) {
                elements.add(TERTIARY_RGBA_4UB);
                elements.add(TERTIARY_TEX_2F);
            }
        }
        
        return new MaterialVertexFormat(elements);
    }
    
    @SuppressWarnings("null")
    public static void encodeBlock(QuadViewImpl q, RenderMaterialImpl.Value mat, ShaderContext context, VertexCollector output, BlockPos pos, float[] aoData) {
        final int shaderFlags = mat.shaderFlags() << 16;
        final int shaderProps = output.materialState().shaderProps;
        final int depth = mat.spriteDepth();
        assert depth == ShaderProps.spriteDepth(shaderProps);
        
        final boolean fatMaps = (shaderProps & ShaderProps.SMOOTH_LIGHTMAPS) != 0;
        
        LightmapHd blockMap = fatMaps ? q.blockLight : null;
        LightmapHd skyMap = fatMaps ? q.skyLight : null;;
            
        for(int i = 0; i < 4; i++) {
            output.pos(pos, q.x(i), q.y(i), q.z(i));
            if((shaderProps & ShaderProps.WHITE_0) == 0) {
                output.add(q.spriteColor(i, 0));
            }
            output.add(q.spriteU(i, 0));
            output.add(q.spriteV(i, 0));
            int packedLight = q.lightmap(i);
            int blockLight = (packedLight & 0xFF);
            int skyLight = ((packedLight >>> 16) & 0xFF);
            output.add(blockLight | (skyLight << 8) | shaderFlags);
            
            if(fatMaps) {
                output.add(blockMap.coord(q, i));
                output.add(skyMap.coord(q, i));
                output.add(0);
            }
            
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
