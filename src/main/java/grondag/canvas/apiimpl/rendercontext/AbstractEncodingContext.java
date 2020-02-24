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


package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

import grondag.canvas.buffer.encoding.EncodingContext;

/**
 * Base quad-rendering class for fallback and mesh consumers.
 * Has most of the actual buffer-time lighting and coloring logic.
 */
public abstract class AbstractEncodingContext implements EncodingContext {
	static final int FULL_BRIGHTNESS = 0xF000F0;

	protected final Function<RenderLayer, VertexConsumer> bufferFunc;
	public final QuadTransform transform;
	protected final Vector3f normalVec = new Vector3f();
	public final Predicate<Direction> cullTest;

	@Override
	public abstract Matrix4f matrix();

	@Override
	public abstract Matrix3f normalMatrix();

	@Override
	public abstract int overlay();

	@Override
	public Vector3f normalVec() {
		return normalVec;
	}

	public AbstractEncodingContext(Function<RenderLayer, VertexConsumer> bufferFunc, QuadTransform transform, Predicate<Direction> cullTest) {
		this.bufferFunc = bufferFunc;
		this.transform = transform;
		this.cullTest = cullTest;
	}
}
