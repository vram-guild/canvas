/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

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
    
    @Inject(at = @At("HEAD"), method = "applyLightmap")
    private void onApplyLightmap(CallbackInfo ci) {
        AbstractClientPlayerEntity player = this.client.player;
        ItemRenderContext.playerLightmap(this.client.world.getLightmapIndex(new BlockPos(player.x, player.y + (double)player.getStandingEyeHeight(), player.z), 0));
    }
}
