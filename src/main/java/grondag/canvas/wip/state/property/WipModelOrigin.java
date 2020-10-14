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

package grondag.canvas.wip.state.property;

// WIP: what to do with this?
// In main world render, the GL matrix is set to be a projection matrix onLy - no view component
// The base matrix stack has view rotation but not translation

// In terrain render, render state is set to include offset and view rotation

// Entity rendering handles the translation as part of buffering and applies
// the matrix stack (with view rotation) on CPU so does not need to be a part of render state

// particle/debug render the CPU includes translation but the view rotation is handled in GPU

public enum WipModelOrigin {
	/**
	 * Vertex coordinate are raw coordinates.
	 * Will need a matrix update per draw.
	 */
	SELF,

	/**
	 * Vertex coordinates are relative to a world region.
	 * Used in terrain rendering. Canvas regions may be 16x16 or 256x256.
	 */
	REGION,

	/**
	 * Vertex coordinates are relative to the camera.
	 * Common for most per-frame render (entities, etc.)
	 */
	CAMERA,

	/**
	 * Vertex coordinates are relative to the screen.
	 * Intended for GUI rendering.
	 */
	SCREEN
}
