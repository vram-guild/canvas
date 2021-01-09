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

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.hud.DebugHud;

import grondag.canvas.buffer.GlBufferAllocator;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;

@Mixin(DebugHud.class)
public class MixinDebugHud {
	@Inject(method = "getLeftText", at = @At("RETURN"), cancellable = false, require = 1)
	private void onGetBufferBuilders(CallbackInfoReturnable<List<String>> ci) {
		final List<String> list = ci.getReturnValue();

		// if (Configurator.hdLightmaps()) {
		// 	list.add("HD Lightmap Occupancy: " + LightmapHd.occupancyReport());
		// }

		list.add(TransferBufferAllocator.debugString());
		list.add(GlBufferAllocator.debugString());
		list.add(VertexCollectorImpl.debugReport());
	}
}
