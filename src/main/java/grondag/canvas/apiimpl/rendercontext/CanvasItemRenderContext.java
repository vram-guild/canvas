/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;

import io.vram.frex.base.renderer.context.render.ItemRenderContext;

import grondag.canvas.apiimpl.rendercontext.encoder.StandardQuadEncoder;
import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderContextState.GuiMode;

public class CanvasItemRenderContext extends ItemRenderContext {
	private static final Supplier<ThreadLocal<CanvasItemRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final CanvasItemRenderContext result = new CanvasItemRenderContext();
		return result;
	});

	private static ThreadLocal<CanvasItemRenderContext> POOL = POOL_FACTORY.get();

	public static void reload() {
		POOL = POOL_FACTORY.get();
	}

	public static CanvasItemRenderContext get() {
		return POOL.get();
	}

	protected VertexConsumer defaultConsumer;

	public final StandardQuadEncoder encoder;

	public CanvasItemRenderContext() {
		super();
		encoder = new StandardQuadEncoder(emitter, inputContext);
	}

	@Override
	protected void encodeQuad() {
		encoder.encode(defaultConsumer);
	}

	@Override
	protected void prepareEncoding(MultiBufferSource vertexConsumers) {
		defaultConsumer = vertexConsumers.getBuffer(inputContext.defaultRenderType());
		encoder.collectors = vertexConsumers instanceof CanvasImmediate ? ((CanvasImmediate) vertexConsumers).collectors : null;
	}

	@Override
	protected void renderCustomModel(BlockEntityWithoutLevelRenderer builtInRenderer, MultiBufferSource vertexConsumers) {
		final ItemStack stack = inputContext.itemStack();
		final RenderContextState context = (vertexConsumers instanceof CanvasImmediate immediate) ? immediate.contextState : null;

		if (context != null) {
			context.setCurrentItem(stack);
			context.guiMode(inputContext.isGui() && inputContext.isBlockItem() && ((BlockItem) stack.getItem()).getBlock() instanceof AbstractBannerBlock ? GuiMode.GUI_FRONT_LIT : GuiMode.NORMAL);
		}

		builtInRenderer.renderByItem(stack, inputContext.mode(), inputContext.matrixStack().toVanilla(), vertexConsumers, inputContext.lightmap(), inputContext.overlay());

		if (context != null) {
			context.setCurrentItem(null);
			context.guiMode(GuiMode.NORMAL);
		}
	}
}
