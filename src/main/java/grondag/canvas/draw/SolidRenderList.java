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

package grondag.canvas.draw;

import java.util.ArrayDeque;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL11;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.buffer.allocation.BindStateManager;
import grondag.canvas.material.old.OldMaterialState;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.ShaderManager;
import grondag.canvas.shader.old.OldShaderContext;
import grondag.canvas.varia.CanvasGlHelper;

/**
 * Accumulates and renders delegates in material-state, buffer order.<p>
 *
 * Note there is no translucent version of this, because translucent
 * must always be rendered in quad-sort order and thus we don't accumulate
 * multiple chunks or models into a single collection.
 */
public class SolidRenderList implements Consumer<ObjectArrayList<DrawableDelegate>> {
	private static final ArrayDeque<SolidRenderList> POOL = new ArrayDeque<>();

	public static SolidRenderList claim() {
		SolidRenderList result = POOL.poll();
		if (result == null) {
			result = new SolidRenderList();
		}
		return result;
	}

	private static class BufferSorter implements IntComparator, Swapper {
		Object[] delegates;

		@Override
		public int compare(int aIndex, int bIndex) {
			final DrawableDelegate a = (DrawableDelegate) delegates[aIndex];
			final DrawableDelegate b = (DrawableDelegate) delegates[bIndex];
			final int matCompare = Long.compare(a.materialState().sortIndex, b.materialState().sortIndex);
			return matCompare == 0 ? Integer.compare(a.bufferId(), b.bufferId()) : matCompare;
		}

		@Override
		public void swap(int a, int b) {
			final Object swap = delegates[a];
			delegates[a] = delegates[b];
			delegates[b] = swap;
		}
	}

	private static final ThreadLocal<BufferSorter> SORTERS = ThreadLocal.withInitial(BufferSorter::new);

	private final ObjectArrayList<DrawableDelegate> delegates = new ObjectArrayList<>();

	private SolidRenderList() {
	}

	@Override
	public void accept(ObjectArrayList<DrawableDelegate> delegatesIn) {
		final int limit = delegatesIn.size();
		for (int i = 0; i < limit; i++) {
			delegates.add(delegatesIn.get(i));
		}
	}

	/**
	 * Renders delegates in buffer order to minimize bind calls.
	 * Assumes all delegates in the list share the same pipeline.
	 */
	public void draw(OldShaderContext context) {
		final int limit = delegates.size();

		if (limit == 0) {
			return;
		}

		final Object[] draws = delegates.elements();

		final BufferSorter sorter = SORTERS.get();
		sorter.delegates = draws;
		Arrays.quickSort(0, limit, sorter, sorter);

		MaterialShaderImpl lastShader = null;
		int lastProps = -1;

		final int frameIndex = ShaderManager.INSTANCE.frameIndex();

		for (int i = 0; i < limit; i++) {
			final DrawableDelegate b = (DrawableDelegate) draws[i];
			final OldMaterialState state = b.materialState();
			final MaterialConditionImpl condition = state.condition;

			if(!condition.affectBlocks || condition.compute(frameIndex)) {
				if(state.shader != lastShader || state.shaderProps != lastProps) {
					state.activate(context);
					lastShader = state.shader;
					lastProps = state.shaderProps;
				}
				b.bind();
				b.draw();
			}
		}
		delegates.clear();
	}

	/**
	 * Cleans up buffer and vertex bindings. Use after non-terrain calls to {@link #draw()}.
	 * Not needed by chunk draw because vanilla already handles.
	 */
	public static void postDrawCleanup() {
		if (CanvasGlHelper.isVaoEnabled()) {
			CanvasGlHelper.glBindVertexArray(0);
		}
		GlStateManager.disableClientState(GL11.GL_VERTEX_ARRAY);
		CanvasGlHelper.enableAttributes(0, true);
		BindStateManager.unbind();
		GlProgram.deactivate();
	}

	public void release() {
		POOL.offer(this);
	}

	public void drawAndRelease(OldShaderContext context) {
		draw(context);
		release();
	}
}
