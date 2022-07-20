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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.base.renderer.context.render.SimpleBlockRenderContext;

import grondag.canvas.apiimpl.rendercontext.encoder.StandardQuadEncoder;

/**
 * Context for non-terrain block rendering.
 */
public class CanvasBlockRenderContext extends SimpleBlockRenderContext {
	private static final Supplier<ThreadLocal<CanvasBlockRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final CanvasBlockRenderContext result = new CanvasBlockRenderContext();
		return result;
	});

	private static ThreadLocal<CanvasBlockRenderContext> POOL = POOL_FACTORY.get();

	public static void reload() {
		POOL = POOL_FACTORY.get();
	}

	public static CanvasBlockRenderContext get() {
		return POOL.get();
	}

	public final StandardQuadEncoder encoder;

	public CanvasBlockRenderContext() {
		super();
		encoder = new StandardQuadEncoder(emitter, inputContext);
	}

	@Override
	public void render(ModelBlockRenderer vanillaRenderer, BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer buffer, boolean checkSides, long seed, int overlay) {
		super.render(vanillaRenderer, blockView, model, state, pos, poseStack, buffer, checkSides, seed, overlay);
		// reset buffer state
		defaultConsumer = null;
	}

	@Override
	protected void encodeQuad() {
		encoder.encode(defaultConsumer);
	}
}
