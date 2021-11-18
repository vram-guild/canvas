/*
 * Copyright © Original Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderBuffers;

import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.mixinterface.RenderBuffersExt;

@Mixin(RenderBuffers.class)
public class MixinRenderBuffers implements RenderBuffersExt {
	@Shadow private BufferSource bufferSource;

	private BufferSource activeBufferSource;

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void onNew(CallbackInfo ci) {
		activeBufferSource = bufferSource;
	}

	/**
	 * @author grondag
	 * @reason simple and reliable
	 */
	@Overwrite
	public MultiBufferSource.BufferSource bufferSource() {
		return activeBufferSource;
	}

	@Override
	public void canvas_setEntityConsumers(CanvasImmediate consumers) {
		activeBufferSource = consumers == null ? bufferSource : consumers;
	}
}
