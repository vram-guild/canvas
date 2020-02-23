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


package grondag.canvas.apiimpl.rendercontext.wip;

import java.util.function.Function;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;

/**
 * Base quad-rendering class for fallback and mesh consumers.
 * Has most of the actual buffer-time lighting and coloring logic.
 */
public abstract class AbstractQuadRenderer2 implements EncoderContext {
	static final int FULL_BRIGHTNESS = 0xF000F0;

	protected final Function<RenderLayer, VertexConsumer> bufferFunc;
	protected final QuadTransform transform;
	protected final BlockRenderInfo blockInfo;
	protected final Vector3f normalVec = new Vector3f();

	@Override
	public abstract Matrix4f matrix();

	@Override
	public abstract Matrix3f normalMatrix();

	@Override
	public abstract int overlay();

	@Override
	public VertexConsumer consumer(MutableQuadViewImpl quad) {
		final RenderLayer layer = blockInfo.effectiveRenderLayer(quad.material().blendMode(0));
		return bufferFunc.apply(layer);
	}

	@Override
	public BlockRenderInfo blockInfo() {
		return blockInfo;
	}

	@Override
	public Vector3f normalVec() {
		return normalVec;
	}

	public AbstractQuadRenderer2(BlockRenderInfo blockInfo, Function<RenderLayer, VertexConsumer> bufferFunc, QuadTransform transform) {
		this.blockInfo = blockInfo;
		this.bufferFunc = bufferFunc;
		this.transform = transform;
	}
}
