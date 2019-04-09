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

import grondag.canvas.varia.BakedQuadExt;
import net.minecraft.client.render.model.BakedQuad;

/**
 * Canvas does shading in GPU, so we need to avoid modifying colors
 * on CPU and also indicate when diffuse should be disabled. This
 * handles the second problem.
 */
@Mixin(BakedQuad.class)
public abstract class MixinBakedQuad implements BakedQuadExt{
    private boolean disableDiffuse = false;

    @Override
    public boolean canvas_disableDiffuse() {
        return disableDiffuse;
    }

    @Override
    public void canvas_disableDiffuse(boolean disable) {
        disableDiffuse = disable;
    }
}
