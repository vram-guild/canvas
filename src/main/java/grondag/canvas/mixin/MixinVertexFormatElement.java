package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.render.VertexFormatElement;

@Mixin(VertexFormatElement.class)
public class MixinVertexFormatElement {
    @Redirect(method = "<init>*", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexFormatElement;isValidType(ILnet/minecraft/client/render/VertexFormatElement$Type;)Z"))
    private boolean onIsValidType(VertexFormatElement caller, int index, VertexFormatElement.Type usage) {
        // has to apply even when mod is disabled so that our formats can be instantiated
        return index == 0 || usage == VertexFormatElement.Type.UV || usage == VertexFormatElement.Type.PADDING;
    }
}
