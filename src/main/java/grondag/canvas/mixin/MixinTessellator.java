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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

import grondag.canvas.buffer.packing.BufferPacker;
import grondag.canvas.buffer.packing.BufferPackingList;
import grondag.canvas.buffer.packing.VertexCollectorList;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.chunk.draw.SolidRenderList;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.salvage.CanvasBufferBuilder;
import grondag.canvas.salvage.TessellatorExt;
import grondag.canvas.shader.old.OldShaderContext;

//TODO: Enable or Remove
@Mixin(Tessellator.class)
public class MixinTessellator implements TessellatorExt {
	@Shadow private BufferBuilder buffer;

	@Redirect(method = "<init>*", require = 1, at = @At(value = "NEW", args = "class=net/minecraft/client/render/BufferBuilder"))
	private BufferBuilder newBuferBuilder(int bufferSizeIn) {
		return new CanvasBufferBuilder(bufferSizeIn);
	}

	@Inject(method = "draw", at = @At("RETURN"), require = 1)
	private void afterDraw(CallbackInfo ci) {
		canvas_draw();
	}

	private OldShaderContext context = OldShaderContext.BLOCK_SOLID;

	@Override
	public void canvas_draw() {
		final CanvasBufferBuilder buffer = (CanvasBufferBuilder)this.buffer;
		final VertexCollectorList vcList = buffer.vcList;
		if(!vcList.isEmpty()) {
			final BufferPackingList packingList = vcList.packingListSolid();
			final SolidRenderList renderList = SolidRenderList.claim();
			buffer.ensureCapacity(packingList.totalBytes());
			final ObjectArrayList<DrawableDelegate> delegates = BufferPacker.pack(packingList, vcList, buffer);
			renderList.accept(delegates);

			//PERF: lightmap tex probably not needed here, or at least make context-dependent
			LightmapHdTexture.instance().enable();
			renderList.draw(context);
			LightmapHdTexture.instance().disable();

			final int limit = delegates.size();
			for(int i = 0; i < limit; i++) {
				delegates.get(i).release();
			}
			SolidRenderList.postDrawCleanup();
			renderList.release();
			vcList.clear();
			buffer.clearAllocations();
		}
	}

	@Override
	public void canvas_context(OldShaderContext context) {
		this.context = context;
	}

	@Override
	public OldShaderContext canvas_context() {
		return context;
	}
}
