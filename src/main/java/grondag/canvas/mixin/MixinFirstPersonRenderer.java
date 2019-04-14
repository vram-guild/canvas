package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.FirstPersonRenderer;
import net.minecraft.util.math.BlockPos;

@Mixin(FirstPersonRenderer.class)
public class MixinFirstPersonRenderer {
    @Shadow private MinecraftClient client;
    
    @Inject(at = @At("HEAD"), method = "applyLightmap", cancellable = true)
    private void onApplyLightmap(CallbackInfo ci) {
        AbstractClientPlayerEntity player = this.client.player;
        ItemRenderContext.playerLightMapIndex(this.client.world.getLightmapIndex(new BlockPos(player.x, player.y + (double)player.getStandingEyeHeight(), player.z), 0));
        ci.cancel();
    }
}
