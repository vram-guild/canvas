package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.canvas.mixinext.BakedQuadExt;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.ModelRotationContainer;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelRotation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;

@Mixin(BakedQuadFactory.class)
public abstract class MixinBakedQuadFactory {
    
    @ModifyArg(method = "bake", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/render/model/BakedQuadFactory;method_3458(Lnet/minecraft/client/render/model/json/ModelElementTexture;Lnet/minecraft/client/texture/Sprite;Lnet/minecraft/util/math/Direction;[FLnet/minecraft/client/render/model/ModelRotation;Lnet/minecraft/client/render/model/json/ModelRotation;Z)[I"))
    private boolean disableShade(boolean shade) {
        return false;
    }
    
    @Inject(method = "bake", at = @At(value = "RETURN"))
    private void hookBake(
            Vector3f vec1, Vector3f vec2, ModelElementFace modelElementFace, 
            Sprite sprite, Direction direction, ModelRotationContainer modelRotationContainer, 
            ModelRotation modelRotation, boolean shade,
            CallbackInfoReturnable<BakedQuad> ci) {
        if(!shade) {
            ((BakedQuadExt)ci.getReturnValue()).canvas_disableDiffuse(true);
        }
    }
}
