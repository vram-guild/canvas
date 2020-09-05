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
import grondag.canvas.material.MaterialVertexFormat;

/**
 * Primitives with the same buffer key can share the same buffer.
 * They should share the same encoder/vertex format, same  and have the same sorting requirements.
 * <p>
 * Content of the buffer should also share the same matrix state but this is
 * not enforced and must be controlled through appropriate usage.
 */
public class MaterialBufferKey {
	public final MaterialVertexFormat format;
	public final VertexEncoder encoder;
	/**
	 * true only for translucent
	 */
	public final boolean sorted;

	private MaterialBufferKey(VertexEncoder encoder, MaterialVertexFormat format, boolean sorted) {
		this.format = format;
		this.encoder = encoder;
		this.sorted = sorted;
	}
}
