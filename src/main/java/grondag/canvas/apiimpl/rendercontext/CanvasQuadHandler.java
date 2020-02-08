/*******************************************************************************
 * Copyright 2019, 2020 grondag
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


package grondag.canvas.apiimpl.rendercontext;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import grondag.canvas.mixinterface.ItemRendererExt;

public class CanvasQuadHandler implements ItemRenderContext.VanillaQuadHandler {
	private final ItemRendererExt itemRenderer;

	public CanvasQuadHandler(ItemRendererExt itemRenderer) {
		this.itemRenderer = itemRenderer;
	}

	@Override
	public void accept(BakedModel model, ItemStack stack, int color, int overlay, MatrixStack matrixStack, VertexConsumer buffer) {
		itemRenderer.canvas_renderBakedItemModel(model, stack, color, overlay, matrixStack, buffer);
	}
}
