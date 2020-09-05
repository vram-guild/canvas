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

/**
 * Primitives with the same DrawState have the same vertex format/buffer,
 * and same uniform state and gl state.
 *
 * Primitives with draw states will be collected in different arrays
 * and then packed into shared buffers (if applicable) with the same buffer key.
 *
 * Order of packing will be a hierarchy of Gl state and uniform state
 */
public class MaterialDrawState {
	public final MaterialBufferKey bufferKey;
	public final MaterialGlState drawState;
	public final MaterialUniformState uniformState;

	private MaterialDrawState(MaterialBufferKey bufferKey, MaterialGlState drawState, MaterialUniformState uniformState) {
		this.bufferKey = bufferKey;
		this.drawState = drawState;
		this.uniformState = uniformState;
	}
}
