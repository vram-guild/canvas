/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.mixin;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import grondag.canvas.material.state.MojangShaderData;
import grondag.canvas.mixinterface.ShaderExt;

@Mixin(RenderStateShard.ShaderStateShard.class)
public class MixinShaderPhase implements ShaderExt {
	@Shadow private Optional<Supplier<ShaderInstance>> supplier;

	@Override
	public MojangShaderData canvas_shaderData() {
		return supplier.isPresent() ? ((ShaderExt) (supplier.get().get())).canvas_shaderData() : MojangShaderData.MISSING;
	}
}
