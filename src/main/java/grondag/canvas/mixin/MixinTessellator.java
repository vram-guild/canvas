package grondag.canvas.mixin;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.buffering.BufferPacker;
import grondag.canvas.buffering.BufferPackingList;
import grondag.canvas.core.CanvasBufferBuilder;
import grondag.canvas.core.Program;
import grondag.canvas.core.SolidRenderList;
import grondag.canvas.core.VertexCollectorList;
import grondag.canvas.opengl.CanvasGlHelper;
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
        final CanvasBufferBuilder buffer = (CanvasBufferBuilder)this.buffer;
        final VertexCollectorList vcList = buffer.vcList;
        if(!vcList.isEmpty()) {
            final BufferPackingList packingList = vcList.packingListSolid();
            final SolidRenderList renderList = SolidRenderList.claim();
            buffer.ensureCapacity(packingList.totalBytes());
            renderList.accept(BufferPacker.pack(packingList, vcList, buffer));
            renderList.drawAndRelease();
            
            // UGLY - really should be part of render list draw but for chunks don't want to do this until end
            GlStateManager.disableClientState(GL11.GL_VERTEX_ARRAY);
            CanvasGlHelper.resetAttributes();
            Program.deactivate();
            
            vcList.clear();
            buffer.clearAllocations();
        }
    }
}
