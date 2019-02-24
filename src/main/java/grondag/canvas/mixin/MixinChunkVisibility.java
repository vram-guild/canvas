package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;

import grondag.canvas.hooks.VisibilityMap;
import grondag.canvas.mixinext.ChunkVisibility;
import net.minecraft.class_854;

@Mixin(class_854.class)
public abstract class MixinChunkVisibility implements ChunkVisibility {
    private Object visibilityData = null;

    @Override
    public Object getVisibilityData() {
        return visibilityData;
    }

    @Override
    public void setVisibilityData(Object data) {
        releaseVisibilityData();
        visibilityData = data;
    }

    /** reuse arrays to prevent garbage build up */
    @Override
    public void releaseVisibilityData() {
        Object prior = visibilityData;
        if (prior != null && prior instanceof VisibilityMap) {
            VisibilityMap.release((VisibilityMap) prior);
            visibilityData = null;
        }
    }

    @Override
    protected void finalize() {
        releaseVisibilityData();
    }
}
