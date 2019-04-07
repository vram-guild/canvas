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
