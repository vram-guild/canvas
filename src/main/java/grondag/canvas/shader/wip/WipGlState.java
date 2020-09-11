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


public class WipGlState {

	public static class Builder {
		// WIP: transparency
		// WIP: depth test
		// WIP: cull
		// WIP: enable lightmap
		// WIP: framebuffer target(s) - add emissive and other targets to vanilla
		// WIP: write mask state
		// WIP: line width
		// WIP: texture binding
		// WIP: texture setting - may need to be uniform or conditional compile if fixed pipeline filtering doesn't work
		// sets up outline, glint or default texturing
		// these probably won't work as-is with shaders because they use texture env settings
		// so may be best to leave them for now

		protected boolean sorted = false;

		public Builder sorted(boolean sorted) {
			this.sorted = sorted;
			return this;
		}
	}
}
