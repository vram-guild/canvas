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

package grondag.canvas.buffer;

import java.nio.IntBuffer;

import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.format.CanvasVertexFormat;

public class StreamDrawBuffer extends AbstractDrawBuffer {
	protected StreamDrawBuffer(int bytes) {
		super(bytes);
	}

	//	/** MUST be called if one of other release methods isn't. ALWAYS returns null. */
	//	@Nullable
	//	StreamBuffer release();

	public static @Nullable StreamDrawBuffer allocate(int bytes, CanvasVertexFormat standardMaterialFormat) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntBuffer intBuffer() {
		return null;
	}

	/** Un-map and bind for drawing. */
	@Override
	public void upload() {
		//
	}

	@Override
	public void bind() {
		//
	}

	@Override
	protected void onShutdown() {
		// TODO Auto-generated method stub
	}
}
