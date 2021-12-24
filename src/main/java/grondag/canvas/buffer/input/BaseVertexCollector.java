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

package grondag.canvas.buffer.input;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.material.state.RenderState;

public abstract class BaseVertexCollector implements DrawableVertexCollector {
	protected final RenderState renderState;
	protected final int quadStrideInts;
	protected final int vertexStrideInts;
	protected final int[] target;
	protected int integerSize = 0;

	public BaseVertexCollector(RenderState renderState, int[] target) {
		this.renderState = renderState;
		this.quadStrideInts = target.length;
		this.target = target;
		this.vertexStrideInts = quadStrideInts / 4;
	}

	@Override
	public final RenderState renderState() {
		return renderState;
	}

	@Override
	public final int integerSize() {
		return integerSize;
	}

	@Override
	public final int byteSize() {
		return integerSize * 4;
	}

	@Override
	public final int quadCount() {
		return integerSize / quadStrideInts;
	}

	@Override
	public final int vertexCount() {
		return integerSize / vertexStrideInts;
	}

	/**
	 * Returns count of vertices that should render in both
	 * color and shadow pass. And vertices after this should
	 * not render in shadow pass.
	 *
	 * <p>Will always be <= {@link #vertexCount()}.
	 */
	public int commonVertexCount() {
		return vertexCount();
	}

	@Override
	public final boolean isEmpty() {
		return integerSize == 0;
	}

	@Override
	public final int[] target() {
		return target;
	}

	@Override
	public void clear() {
		integerSize = 0;
	}

	@Override
	public void commit(int effectiveFaceIndex, boolean castShadow) {
		commit(quadStrideInts);
	}

	@Override
	public void commit(boolean castShadow) {
		commit(quadStrideInts);
	}

	@Override
	public final void draw(boolean clear) {
		if (!isEmpty()) {
			drawSingle();

			if (clear) {
				clear();
			}
		}
	}

	/** Avoid: slow. */
	public final void drawSingle() {
		// PERF: allocation - or eliminate this
		final ObjectArrayList<DrawableVertexCollector> drawList = new ObjectArrayList<>();
		drawList.add(this);
		DrawableVertexCollector.draw(drawList);
	}
}
