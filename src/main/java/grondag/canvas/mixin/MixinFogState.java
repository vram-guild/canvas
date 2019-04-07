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

//TODO: remove?

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.mixinext.AccessFogState;
import grondag.canvas.mixinext.FogStateHolder;

@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager$FogState")
public abstract class MixinFogState implements AccessFogState {
    @Override
    @Accessor
    public abstract int getMode();

    @Override
    @Accessor
    public abstract float getDensity();

    @Override
    @Accessor
    public abstract float getStart();

    @Override
    @Accessor
    public abstract float getEnd();
    
    @Inject(method = "<init>*", require = 1, at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci) {
        FogStateHolder.INSTANCE = ((AccessFogState) (Object) this);
    }
}
