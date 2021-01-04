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

package grondag.canvas.pipeline.pass;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.pipeline.config.PassConfig;

public class ClearPass extends Pass {
	ClearPass(PassConfig config) {
		super(config);
	}

	@Override
	public void run(int width, int height) {
		if (fbo != null) {
			fbo.bind();
			RenderSystem.viewport(0, 0, width, height);
			fbo.clear();
		}
	}

	@Override
	public void close() {
		// NOOP
	}
}
