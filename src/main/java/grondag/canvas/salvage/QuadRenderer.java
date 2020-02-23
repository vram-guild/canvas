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

package grondag.canvas.salvage;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.mesh.QuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.packing.VertexCollector;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.ShaderContext;
import grondag.canvas.material.VertexEncodingContext;

/**
 * Base quad-rendering class for fallback and mesh consumers. Has most of the
 * actual buffer-time lighting and coloring logic.
 */
public class QuadRenderer {
	public static final Consumer<MutableQuadViewImpl> NO_OFFSET = (q) -> {};

	protected final ToIntFunction<BlockPos> brightnessFunc;
	protected final BiFunction<RenderMaterialImpl.Value, QuadViewImpl, VertexCollector> collectorFunc;
	protected final BlockRenderInfo blockInfo;
	protected final AoCalculator aoCalc;
	protected final BooleanSupplier hasTransform;
	protected final QuadTransform transform;
	protected final Consumer<MutableQuadViewImpl> offsetFunc;
	protected final Function<RenderMaterialImpl.Value, ShaderContext> contextFunc;
	QuadRenderer(
			BlockRenderInfo blockInfo,
			ToIntFunction<BlockPos> brightnessFunc,
			BiFunction<RenderMaterialImpl.Value, QuadViewImpl, VertexCollector> collectorFunc,
			AoCalculator aoCalc,
			BooleanSupplier hasTransform,
			QuadTransform transform,
			Consumer<MutableQuadViewImpl> offsetFunc,
			Function<RenderMaterialImpl.Value, ShaderContext> contextFunc) {
		this.blockInfo = blockInfo;
		this.brightnessFunc = brightnessFunc;
		this.collectorFunc = collectorFunc;
		this.aoCalc = aoCalc;
		this.hasTransform = hasTransform;
		this.transform = transform;
		this.offsetFunc = offsetFunc;
		this.contextFunc = contextFunc;
	}

	/** handles block color and red-blue swizzle, common to all renders */
	private void colorizeQuad(MutableQuadViewImpl q) {
		//final int blockColorIndex = q.colorIndex();
		//ColorHelper.colorizeQuad(q, blockColorIndex == -1 ? -1 : (blockInfo.blockColor(blockColorIndex)));
	}

	/**
	 * Use when transform and face call have already been applied/checked.
	 */
	protected void renderQuadInner(final MutableQuadViewImpl q) {
		final RenderMaterialImpl.Value mat = null; //q.material().forRenderLayer(blockInfo..defaultLayerIndex);
		final boolean isAo = true; //blockInfo.defaultAo && mat.hasAo;

		if (isAo) {
			// needs to happen before offsets are applied
			aoCalc.compute(q);
		}

		offsetFunc.accept(q);

		colorizeQuad(q);

		if(isAo) {
			lightSmooth(q);
		} else {
			lightFlat(q);
		}

		final VertexCollector output = collectorFunc.apply(mat, q);

		encodeQuad(q, output, mat, isAo);
	}

	private final VertexEncodingContext encodingContext = new VertexEncodingContext();

	private void encodeQuad(MutableQuadViewImpl q, VertexCollector output, RenderMaterialImpl.Value mat, boolean isAo) {
		final int shaderFlags = isAo ? mat.shaderFlags() : mat.shaderFlags() | RenderMaterialImpl.SHADER_FLAGS_DISABLE_AO;
		output.materialState().materialVertexFormat().encode(q, encodingContext.prepare(mat, contextFunc.apply(mat), blockInfo.blockPos, isAo ? aoCalc.ao : null, shaderFlags), output);
	}

	/** for non-emissive mesh quads and all fallback quads with smooth lighting */
	private void lightSmooth(MutableQuadViewImpl q) {
		for (int i = 0; i < 4; i++) {
			q.lightmap(i, ColorHelper.maxBrightness(q.lightmap(i), aoCalc.light[i]));
		}
	}

	/** for non-emissive mesh quads and all fallback quads with flat lighting */
	private void lightFlat(MutableQuadViewImpl quad) {
		final int brightness = flatBrightness(quad, blockInfo.blockState, blockInfo.blockPos);
		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), brightness));
		}
		//UGLY: ugly hack is ugly (and slow)
		quad.aoShade = null;
		quad.blockLight = null;
		quad.skyLight = null;
	}

	private final BlockPos.Mutable mpos = new BlockPos.Mutable();

	/**
	 * Handles geometry-based check for using self brightness or neighbor
	 * brightness. That logic only applies in flat lighting.
	 */
	private int flatBrightness(MutableQuadViewImpl quad, BlockState blockState, BlockPos pos) {
		// vanilla compatibility hack
		// For flat lighting, cull face is always used instead of light face.
		final Direction cullFace = quad.cullFace();
		mpos.set(pos);
		if (cullFace != null) {
			mpos.setOffset(cullFace);
		}
		// Unfortunately cannot use cache for flat lighting because of magma blocks
		return 0; //blockState.getBlockBrightness(blockInfo.blockView, mpos);
	}
}
