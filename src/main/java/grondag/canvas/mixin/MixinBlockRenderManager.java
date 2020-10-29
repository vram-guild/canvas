/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.mixin;

import grondag.canvas.apiimpl.rendercontext.EntityBlockRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

@Mixin(BlockRenderManager.class)
public abstract class MixinBlockRenderManager {
	@Shadow private BlockModelRenderer blockModelRenderer;

	/**
	 * @reason performance; less bad than inject and cancel at head
	 */
	@Overwrite
	public void renderBlockAsEntity(BlockState state, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		final BlockRenderType blockRenderType = state.getRenderType();
		if (blockRenderType != BlockRenderType.INVISIBLE) {
			switch(blockRenderType) {
				case MODEL:
					final BakedModel bakedModel = ((BlockRenderManager)(Object) this).getModel(state);
					EntityBlockRenderContext.get().render(blockModelRenderer, bakedModel, state, matrices, consumers, overlay, light);
					break;
				case ENTITYBLOCK_ANIMATED:
					BuiltinModelItemRenderer.INSTANCE.render(new ItemStack(state.getBlock()), ModelTransformation.Mode.NONE, matrices, consumers, light, overlay);
					break;
				default:
					break;
			}
		}
	}
}
