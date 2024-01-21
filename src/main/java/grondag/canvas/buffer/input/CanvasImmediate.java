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

import java.util.Map;
import java.util.SortedMap;

import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;

import io.vram.frex.api.buffer.VertexEmitter;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.rendertype.RenderTypeExclusion;
import io.vram.frex.api.rendertype.RenderTypeUtil;

import grondag.canvas.buffer.util.DrawableStream;
import grondag.canvas.material.property.TargetRenderState;
import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.mixinterface.RenderTypeExt;

public class CanvasImmediate extends BufferSource {
	public final VertexCollectorList collectors = new VertexCollectorList(false, false);
	public final RenderContextState contextState;
	private boolean allowEndBatch = true;

	/**
	 * We want to protect buffers from being drawn too early due to {@link BufferSource} method calls.
	 * Note that excluded render types are unprotected.
	 */
	public static void protectBuffers(CanvasImmediate... buffers) {
		for (CanvasImmediate i:buffers) {
			i.allowEndBatch = false;
		}
	}

	public static void unprotectBuffers(CanvasImmediate... buffers) {
		for (CanvasImmediate i:buffers) {
			i.allowEndBatch = true;
		}
	}

	public CanvasImmediate(BufferBuilder fallbackBuffer, Map<RenderType, BufferBuilder> layerBuffers, RenderContextState contextState) {
		super(fallbackBuffer, layerBuffers);
		this.contextState = contextState;
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderLayer) {
		CanvasRenderMaterial mat = ((RenderTypeExt) renderLayer).canvas_materialState();

		if (mat.isMissing()) {
			return super.getBuffer(renderLayer);
		}

		mat = contextState.mapMaterial(mat);

		if (mat.isMissing()) {
			return super.getBuffer(renderLayer);
		} else {
			return collectors.emitter.prepare(mat);
		}
	}

	public VertexEmitter getConsumer(RenderMaterial material) {
		final CanvasRenderMaterial mat = contextState.mapMaterial((CanvasRenderMaterial) material);
		return collectors.emitter.prepare(mat);
	}

	public DrawableStream prepareDrawable(TargetRenderState target) {
		final ObjectArrayList<DrawableVertexCollector> drawList = collectors.sortedDrawList(target);

		return drawList.isEmpty() ? DrawableStream.EMPTY : new DrawableStream(drawList);
	}

	public void drawCollectors(TargetRenderState target) {
		final ObjectArrayList<DrawableVertexCollector> drawList = collectors.sortedDrawList(target);

		if (!drawList.isEmpty()) {
			DrawableVertexCollector.draw(drawList);
		}
	}

	@Override
	public void endBatch() {
		endBatchInner(false);
	}

	/**
	 * Well-understood usage should call this instead of {@link #endBatch()}.
	 */
	public void endBatchSafely() {
		endBatchInner(true);
	}

	private void endBatchInner(boolean force) {
		if (allowEndBatch || force) {
			final ObjectArrayList<DrawableVertexCollector> drawList = collectors.sortedDrawList(Predicates.alwaysTrue());

			if (!drawList.isEmpty()) {
				DrawableVertexCollector.draw(drawList);
			}
		}

		super.endBatch();
	}

	@Override
	public void endBatch(RenderType renderType) {
		endBatchInner(renderType, false);
	}

	/**
	 * Well-understood usage should call this instead of {@link #endBatch(RenderType)}.
	 */
	public void endBatchSafely(RenderType renderType) {
		endBatchInner(renderType, true);
	}

	private void endBatchInner(RenderType renderType, boolean force) {
		if (RenderTypeExclusion.isExcluded(renderType)) {
			super.endBatch(renderType);
		} else if (allowEndBatch || force) {
			final DrawableVertexCollector collector = collectors.getIfExists((CanvasRenderMaterial) RenderTypeUtil.toMaterial(renderType));

			if (collector != null && !collector.isEmpty()) {
				collector.draw(true);
			}
		}
	}

	public static SortedMap<RenderType, BufferBuilder> entityBuilders() {
		return Util.make(new Object2ObjectLinkedOpenHashMap<>(), (object2ObjectLinkedOpenHashMap) -> {
			// Shadows don't like sharing buffers with leash
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, EntityRenderDispatcher.SHADOW_RENDER_TYPE);

			// assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.translucentNoCrumbling());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.armorGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.armorEntityGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.glint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.glintDirect());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.glintTranslucent());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.entityGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.entityGlintDirect());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.waterMask());
			ModelBakery.DESTROY_TYPES.forEach((renderLayer) -> {
				assignBufferBuilder(object2ObjectLinkedOpenHashMap, renderLayer);
			});
		});
	}

	private static void assignBufferBuilder(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> builderStorage, RenderType layer) {
		builderStorage.put(layer, new BufferBuilder(layer.bufferSize()));
	}
}
