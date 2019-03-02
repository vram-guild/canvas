package grondag.canvas.core;

import static grondag.canvas.core.PipelineVertexFormat.*;

public enum LightingModel {
    CLASSIC(SINGLE, DOUBLE, TRIPLE);

//    EHNANCED(ENHANCED_SINGLE, ENHANCED_DOUBLE, ENHANCED_TRIPLE)
//    {
//        @Override
//        public CompoundVertexLighter createLighter()
//        {
//            return new EnhancedVertexLighter();
//        }
//    };

    private LightingModel(PipelineVertexFormat... formatMap) {
        this.formatMap = formatMap;
    }

    private final PipelineVertexFormat[] formatMap;

    public PipelineVertexFormat vertexFormat(int spriteDepth) {
        return formatMap[spriteDepth - 1];
    }
}
