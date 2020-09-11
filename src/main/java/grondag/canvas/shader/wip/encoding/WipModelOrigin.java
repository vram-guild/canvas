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

package grondag.canvas.shader.wip.encoding;

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
