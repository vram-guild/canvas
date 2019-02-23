package grondag.acuity.core;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;

import grondag.acuity.api.IBlockInfo;
import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IPipelinedQuadConsumer;
import grondag.acuity.api.IPipelinedVertexConsumer;
import grondag.acuity.api.IRenderPipeline;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.TextureFormat;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VanillaQuadWrapper implements IPipelinedQuad
{
    private final IRenderPipeline SIMPLE = PipelineManager.INSTANCE.getDefaultPipeline(TextureFormat.SINGLE);
    private @Nullable BakedQuad wrapped;
    private @Nullable BlockRenderLayer layer;
    private final float[][] positions = new float[4][3];
    private final float[] unpack = new float[3];
    private final Vector3f v0 = new Vector3f();
    private final Vector3f v1 = new Vector3f();
    private final Vector3f v2 = new Vector3f();
    
    protected boolean enableAmbientOcclusion = true;
    
    public void prepare(BlockRenderLayer layer, boolean enableAmbientOcclusion)
    {
        this.layer = layer;
        this.enableAmbientOcclusion = enableAmbientOcclusion;
    }
    
    @Override
    public @Nullable IRenderPipeline getPipeline()
    {
        return SIMPLE;
    }

    @SuppressWarnings({"null" })
    @Override
    public void produceVertices(IPipelinedVertexConsumer vertexLighter)
    {
        if(!wrapped.shouldApplyDiffuseLighting())
            vertexLighter.setShading(false);
                     
        if(!this.enableAmbientOcclusion)
            vertexLighter.setAmbientOcclusion(false);
        
        final int blockColor = this.getColorMultiplier(vertexLighter.getBlockInfo());
        
        final int[] data =  wrapped.getVertexData();
        final VertexFormat format = wrapped.getFormat();
        final float[][] pos = this.positions;
        float normX = 0, normY = 1, normZ = 0;
        int normalIndex = -1;
        int uvIndex = -1;
        int lightMapIndex = -1;
        
        for(int i = 0; i < format.getElementCount(); i++)
        {
            VertexFormatElement e = format.getElement(i);
            switch(e.getUsage())
            {
            case NORMAL:
                normalIndex = i;
                break;
                
            case UV:
                if(e.getIndex() == 0)
                    uvIndex = i;
                else
                    lightMapIndex = i;
                break;
                
            default:
                break;
            }
        }
        
        for(int i = 0; i < 4; i++)
        {
            LightUtil.unpack(data, pos[i], format, i, 0);
        }
        
        if(normalIndex == -1)
        {
            final Vector3f v0 = this.v0;
            final Vector3f v1 = this.v1;
            final Vector3f v2 = this.v2;
            
            v0.set(pos[3]);
            v1.set(pos[1]);
            v2.set(pos[2]);
            
            v0.sub(v1);
            
            v1.set(pos[0]);
            v2.sub(v1);
            v0.cross(v2, v0);
            v0.normalize();
            normX = v0.x;
            normY = v0.y;
            normZ = v0.z;
        }
        
        float[] unpack = this.unpack;
        
        for(int i = 0; i < 4; i++)
        {
            if(normalIndex != -1)
            {
                LightUtil.unpack(data, unpack, format, i, normalIndex);
                normX = unpack[0];
                normY = unpack[1];
                normZ = unpack[2];
            }
            
            if(lightMapIndex != -1)
            {
                LightUtil.unpack(data, unpack, format, i, lightMapIndex);
                
                // convert to match 0-255 lighter expects
                vertexLighter.setSkyLightMap(Math.round(unpack[1] * PipelinedVertexLighter.LIGHTMAP_TO_255));
                int blockLight = Math.round(unpack[0] * PipelinedVertexLighter.LIGHTMAP_TO_255);
                // pass it as white light with 100% flicker
                vertexLighter.setBlockLightMap(blockLight, blockLight, blockLight, 0xFF);
            }
            
            int rawColor = data[(i * format.getSize() + format.getColorOffset()) / 4];
            rawColor = AcuityColorHelper.multiplyColor(rawColor, blockColor);
                    
            LightUtil.unpack(data, unpack, format, i, uvIndex);
            
            vertexLighter.acceptVertex(pos[i][0], pos[i][1], pos[i][2], normX, normY, normZ, rawColor, unpack[0], unpack[1]);
        }
        
    }
    
    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return this.layer;
    }

    public void wrapAndLight(IPipelinedQuadConsumer lighter, BakedQuad q)
    {
        this.wrapped = q;
        lighter.accept(this); 
    }

    public int getColorMultiplier(IBlockInfo iBlockInfo)
    {
        @SuppressWarnings("null")
        final int tint = wrapped.getTintIndex();
        return tint == -1 ? 0xFFFFFFFF : (0xFF000000 | iBlockInfo.getColorMultiplier(tint)); 
    }
}
