/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.remove;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.NormalHelper;

/**
 * Encodes quads according to a specific GPU-side vertex format/shader/lighting scheme.
 * Also provides compatibility handlers for Mojang's vertex consumer interface.
 */
public abstract class VertexEncoderOld {
	public static final int FULL_BRIGHTNESS = 0xF000F0;

	/**
	 * Determines color index and render layer, then routes to appropriate
	 * tesselate routine based on material properties.
	 */
	public abstract void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context);

	public final void vertex(VertexCollectorImplOld collector, double x, double y, double z) {
		collector.addf((float) x);
		collector.addf((float) y);
		collector.addf((float) z);
	}

	public final void vertex(VertexCollectorImplOld collector, float x, float y, float z, float i, float j, float k, float l, float m, float n, int o, int p, float q, float r, float s) {
		collector.addf(x);
		collector.addf(y);
		collector.addf(z);
		collector.color(i, j, k, l);
		collector.texture(m, n);
		collector.overlay(o);
		collector.light(p);
		collector.normal(q, r, s);
	}

	public final void color(VertexCollectorImplOld collector, int r, int g, int b, int a) {
		collector.addi((r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | ((a & 0xFF) << 24));
	}

	public final void texture(VertexCollectorImplOld collector, float u, float v) {
		collector.addf(u);
		collector.addf(v);
	}

	public final void overlay(VertexCollectorImplOld collector, int s, int t) {
		// TODO: disabled for now - needs to be controlled by format because is called when not present
		//add((s & 0xFFFF) | ((t & 0xFFFF) << 16));
	}

	public void light(VertexCollectorImplOld collector, int blockLight, int skyLight) {
		collector.addi((blockLight & 0xFFFF) | ((skyLight & 0xFFFF) << 16));
	}

	public final void normal(VertexCollectorImplOld collector, float x, float y, float z) {
		collector.addi(NormalHelper.packNormal(x, y, z, 1));
	}
}
