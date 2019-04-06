package grondag.canvas.core;

import grondag.canvas.RenderConditionImpl;
import grondag.fermion.varia.Useful;

public class ConditionalPipeline {
    private static final int PIPELINE_SHIFT = Useful.bitLength(RenderConditionImpl.MAX_CONDITIONS);
    public static final int MAX_CONDITIONAL_PIPELINES = RenderConditionImpl.MAX_CONDITIONS * PipelineManager.MAX_PIPELINES;

    private static int computeIndex(RenderPipelineImpl pipeline, RenderConditionImpl condition) {
        return (pipeline.getIndex() << PIPELINE_SHIFT) | condition.index;
    }
    
    private static final ConditionalPipeline[] VALUES = new ConditionalPipeline[MAX_CONDITIONAL_PIPELINES];
    
    public static ConditionalPipeline get(int index) {
        return VALUES[index];
    }
    
    public static ConditionalPipeline get(RenderPipelineImpl pipeline, RenderConditionImpl condition) {
        final int index = computeIndex(pipeline, condition);
        ConditionalPipeline result = VALUES[index];
        if(result == null) {
            synchronized(VALUES) {
                result = VALUES[index];
                if(result == null) {
                    result = new ConditionalPipeline(pipeline, condition, index);
                    VALUES[index] = result;
                }
            }
        }
        return result;
    }
    
    public final RenderPipelineImpl pipeline;
    public final RenderConditionImpl condition;
    public final int index;
    
    private ConditionalPipeline(RenderPipelineImpl pipeline, RenderConditionImpl condition, int index) {
        this.pipeline = pipeline;
        this.condition = condition;
        this.index = index;
    }
}
