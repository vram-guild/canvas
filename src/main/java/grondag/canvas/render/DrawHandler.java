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

package grondag.canvas.render;

import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.shader.ShaderPass;
import grondag.canvas.varia.CanvasGlHelper;

public abstract class DrawHandler {
	private static DrawHandler current = null;

	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	public final MaterialVertexFormat format;
	public final ShaderPass shaderPass;

	DrawHandler(MaterialVertexFormat format, ShaderPass shaderPass) {
		assert shaderPass != ShaderPass.PROCESS;
		this.format = format;
		this.shaderPass = shaderPass;
	}

	public static void teardown() {
		if (current != null) {
			current.teardownInner();
			current = null;
		}
	}

	public final void setup() {
		final DrawHandler d = current;

		if (d == null) {
			// PERF: really needed?  Doesn't seem to help or hurt
			// Important this happens BEFORE anything that could affect vertex state
			if (CanvasGlHelper.isVaoEnabled()) {
				CanvasGlHelper.glBindVertexArray(0);
			}

			setupInner();
			current = this;
		} else if (d != this) {
			// Important this happens BEFORE anything that could affect vertex state
			if (CanvasGlHelper.isVaoEnabled()) {
				CanvasGlHelper.glBindVertexArray(0);
			}

			d.teardownInner();
			setupInner();
			current = this;
		}
	}

	protected abstract void setupInner();
	//state.activate(OldShaderContext.BLOCK_TRANSLUCENT);

	protected abstract void teardownInner();
}
