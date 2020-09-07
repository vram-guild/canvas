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

package grondag.canvas.shader.wip;

import grondag.canvas.buffer.encoding.VertexEncoder;

/**
 * Primitives with the same key can share the same vertex collector and the same draw call.
 * For this to work they must share the same format and uniform state.<p>
 *
 * Primitives must have the same sorting requirements, which for all but the translucent
 * collection keys means there is no sorting. Translucent primitives that require sorting
 * all belong to a small handful of collectors.
 */
public class MaterialEncodingKey {
	public final VertexEncoder encoder;
	/**
	 * true only for translucent
	 */
	public final boolean sorted;

	private MaterialEncodingKey(VertexEncoder encoder, MaterialUniformState uniformState, boolean sorted) {
		this.encoder = encoder;
		this.sorted = sorted;
	}
}
