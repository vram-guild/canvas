package grondag.acuity.core;

import static grondag.acuity.core.PipelineVertexFormat.*;

import grondag.acuity.api.TextureFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum LightingModel
{
    CLASSIC(VANILLA_SINGLE, VANILLA_DOUBLE, VANILLA_TRIPLE);
    
//    EHNANCED(ENHANCED_SINGLE, ENHANCED_DOUBLE, ENHANCED_TRIPLE)
//    {
//        @Override
//        public CompoundVertexLighter createLighter()
//        {
//            return new EnhancedVertexLighter();
//        }
//    };
    
    private LightingModel(PipelineVertexFormat... formatMap)
    {
        this.formatMap = formatMap;
    }
    
    private final PipelineVertexFormat[] formatMap;
    
    public PipelineVertexFormat vertexFormat(TextureFormat textureFormat)
    {
        return formatMap[textureFormat.ordinal()];
    }

    public CompoundVertexLighter createLighter()
    {
        return new VanillaVertexLighter();
    }
}
