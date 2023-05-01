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

import net.minecraft.client.renderer.MultiBufferSource;

import io.vram.frex.base.renderer.context.render.EntityBlockRenderContext;

import grondag.canvas.apiimpl.rendercontext.encoder.StandardQuadEncoder;
import grondag.canvas.buffer.input.CanvasImmediate;

/**
 * Context used when blocks are rendered as part of an entity.
 * Vanilla examples include blocks held be endermen, blocks in minecarts,
 * flowers held by iron golems and Mooshroom mushrooms.
 *
 * <p>Also handle rendering of the item frame which looks and acts like a block
 * and has a block JSON model but is an entity.
 */
public class CanvasEntityBlockRenderContext extends EntityBlockRenderContext {
	private static final Supplier<ThreadLocal<CanvasEntityBlockRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final CanvasEntityBlockRenderContext result = new CanvasEntityBlockRenderContext();
		return result;
	});

	private static ThreadLocal<CanvasEntityBlockRenderContext> POOL = POOL_FACTORY.get();

	public final StandardQuadEncoder encoder;

	public CanvasEntityBlockRenderContext() {
		super();
		encoder = new StandardQuadEncoder(emitter, inputContext);
	}

	public static void reload() {
		POOL = POOL_FACTORY.get();
	}

	public static CanvasEntityBlockRenderContext get() {
		return POOL.get();
	}

	@Override
	protected void shadeQuad() {
		emitter.applyFlatLighting(inputContext.flatBrightness(emitter));
		emitter.colorize(inputContext);
	}

	@Override
	protected void encodeQuad() {
		encoder.encode(defaultConsumer);
	}

	@Override
	protected void prepareEncoding(MultiBufferSource vertexConsumers) {
		encoder.collectors = vertexConsumers instanceof CanvasImmediate ? ((CanvasImmediate) vertexConsumers).collectors : null;
	}
}
