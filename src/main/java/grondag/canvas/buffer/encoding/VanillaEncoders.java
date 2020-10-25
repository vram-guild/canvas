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

package grondag.canvas.buffer.encoding;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;

import static grondag.canvas.buffer.encoding.EncoderUtils.applyBlockLighting;
import static grondag.canvas.buffer.encoding.EncoderUtils.applyItemLighting;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuad;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuadDirect;
import static grondag.canvas.buffer.encoding.EncoderUtils.colorizeQuad;

public class VanillaEncoders {
	public static final VertexEncoder VANILLA_BLOCK = new VertexEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context);
			bufferQuad(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_TERRAIN = new VanillaTerrainEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context);
			bufferQuadDirect(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_ITEM = new VertexEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			colorizeQuad(quad, context);
			applyItemLighting(quad, context);
			bufferQuad(quad, context);
		}
	};

	abstract static class VanillaTerrainEncoder extends VertexEncoder {

		VanillaTerrainEncoder() {
			super();
		}

	}
}
