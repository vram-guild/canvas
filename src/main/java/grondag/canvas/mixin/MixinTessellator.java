package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.core.CanvasBufferBuilder;
import grondag.canvas.core.VertexCollectorList;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

@Mixin(Tessellator.class)
public class MixinTessellator {
    @Shadow private BufferBuilder buffer;
    
    @Redirect(method = "<init>*", require = 1, at = @At(value = "NEW", args = "class=net/minecraft/client/render/BufferBuilder"))
    private BufferBuilder newBuferBuilder(int bufferSizeIn) {
        return new CanvasBufferBuilder(bufferSizeIn);
    }

    @Inject(method = "draw", at = @At("RETURN"), require = 1)
    private void afterDraw(CallbackInfo ci) {
        final VertexCollectorList vcList = ((CanvasBufferBuilder)buffer).vcList;
        if(!vcList.isEmpty()) {
            vcList.clear();
        }
    }
}
