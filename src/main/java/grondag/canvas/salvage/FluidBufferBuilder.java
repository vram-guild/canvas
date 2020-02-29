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

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.BlockPos;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.buffer.packing.old.OldVertexCollector;

public class FluidBufferBuilder extends BufferBuilder {
	private static final int DEFAULT_SHADER_FLAGS = Canvas.INSTANCE.materialFinder().disableAo(0, true).find().shaderFlags() << 16;

	public FluidBufferBuilder() {
		super(256);
	}

	OldVertexCollector vc;
	BlockPos pos;
	private int vertexIndex = 0;
	private final float[] vertex = new float[12];
	private final int[] colors = new int[4];
	private final int[] lightmaps = new int[4];
	private final float[] uv = new float[8];

	public FluidBufferBuilder prepare(OldVertexCollector vc, BlockPos pos, BlendMode layer) {
		this.vc = vc;
		this.pos = pos;
		vertexIndex = 0;
		return this;
	}

	@Override
	public BufferBuilder vertex(double x, double y, double z) {
		//fluid renderer will already include block pos offset so unfortunately we need to remove it
		final int i = vertexIndex * 3;
		vertex[i] = (float)(x - pos.getX());
		vertex[i + 1] = (float)(y - pos.getY());
		vertex[i + 2] = (float)(z - pos.getZ());
		return this;
	}

	//@Override
	public void setOffset(double x, double y, double z) {
		vc.parent.setRelativeRenderOrigin(-x, -y, -z);
		//super.setOffset(x, y, z);
	}

	@Override
	public BufferBuilder color(float r, float g, float b, float a) {
		int color = (int)(b * 255f);
		color |= (int)(g * 255f) << 8;
		color |= (int)(r * 255f) << 16;
		color |= (int)(a * 255f) << 24;
		colors[vertexIndex] = ColorHelper.swapRedBlueIfNeeded(color);
		return this;
	}

	//@Override
	public BufferBuilder texture(double u, double v) {
		final int i = vertexIndex * 2;
		uv[i] = (float)u;
		uv[i + 1] = (float)v;
		return this;
	}

	//@Override
	public BufferBuilder texture(int skyLight, int blockLight) {
		lightmaps[vertexIndex] = (blockLight & 0xFF) | ((skyLight & 0xFF) << 8) | DEFAULT_SHADER_FLAGS;
		return this;
	}

	@Override
	public void next() {
		if(++vertexIndex == 4) {
			final int normal = packedNormal();
			// Undo diffuse shading - happens in GPU
			float undoShade = 1f;
			if(Math.abs(NormalHelper.getPackedNormalComponent(normal, 0)) == 1) {
				undoShade = 1f / 0.6f;
			} else if(Math.abs(NormalHelper.getPackedNormalComponent(normal, 2)) == 1) {
				undoShade = 1f / 0.8f;
			} else if(NormalHelper.getPackedNormalComponent(normal, 1) == -1) {
				undoShade = 2f;
			}

			int j = 0;
			int k = 0;
			for(int i = 0; i < 4; i++) {
				vc.pos(pos, vertex[j++], vertex[j++], vertex[j++]);
				vc.add(ColorHelper.multiplyRGB(colors[i], undoShade));
				vc.add(uv[k++]);
				vc.add(uv[k++]);
				vc.add(lightmaps[i]);
				vc.add(normal);
			}
			vertexIndex = 0;
		}
	}

	private int packedNormal() {
		final float dx0 = vertex[6] - vertex[0];
		final float dy0 = vertex[7] - vertex[1];
		final float dz0 = vertex[8] - vertex[2];
		final float dx1 = vertex[9] - vertex[3];
		final float dy1 = vertex[10] - vertex[4];
		final float dz1 = vertex[11] - vertex[5];

		float normX = dy0 * dz1 - dz0 * dy1;
		float normY = dz0 * dx1 - dx0 * dz1;
		float normZ = dx0 * dy1 - dy0 * dx1;

		final float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);
		if (l != 0) {
			normX /= l;
			normY /= l;
			normZ /= l;
		}

		return NormalHelper.packNormal(normX, normY, normZ, 1);
	}

	@Override
	public void sortQuads(float float_1, float float_2, float float_3) {
		throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
	}

	//@Override
	public State toBufferState() {
		throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
	}

	@Override
	public void restoreState(State bufferBuilder$State_1) {
		throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
	}

	@Override
	public void begin(int int_1, VertexFormat vertexFormat_1) {
		throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
	}


	@Override
	public BufferBuilder color(int int_1, int int_2, int int_3, int int_4) {
		throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
	}

	@Override
	public BufferBuilder normal(float float_1, float float_2, float float_3) {
		throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
	}

	@Override
	public void end() {
		throw new UnsupportedOperationException("Fluid buffer builder got unexpected method call.");
	}
}
