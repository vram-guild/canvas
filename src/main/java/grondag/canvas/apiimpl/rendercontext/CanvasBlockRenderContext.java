/*
 * Copyright © Contributing Authors
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

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Supplier;

import grondag.canvas.apiimpl.rendercontext.base.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.encoder.StandardQuadEncoder;

/**
 * Context for non-terrain block rendering.
 */
public class CanvasBlockRenderContext extends BlockRenderContext {
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
	protected void encodeQuad() {
		encoder.encode(defaultConsumer);
	}
}
