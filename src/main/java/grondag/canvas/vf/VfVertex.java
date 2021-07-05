/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.vf;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

public class VfVertex extends VfTexture<VertexElement> {
	public static final VfVertex VERTEX = new VfVertex(TextureData.VF_VERTEX, GFX.GL_RGBA32I);

	//final AtomicInteger count = new AtomicInteger();

	private static final ThreadLocal<VertexElement> SEARCH_KEY = ThreadLocal.withInitial(VertexElement::new);

	private VfVertex(int textureUnit, int imageFormat) {
		super(textureUnit, imageFormat, 16);
	}

	/**
	 * Thread-safe.
	 */
	public int index(final Matrix4fExt matrix, Matrix3fExt normalMatrix, MutableQuadViewImpl quad) {
		//count.incrementAndGet();

		// WIP: avoid threadlocal
		final VertexElement k = SEARCH_KEY.get();
		quad.transformAndAppendPackedVertices(matrix, normalMatrix, k.data, 0);
		k.compute();
		return MAP.computeIfAbsent(k, mapFunc).index;
	}

	public VertexElement fromIndex(int index) {
		return (VertexElement) image.elements[index >> 16][index & 0xFFFF];
	}
}
