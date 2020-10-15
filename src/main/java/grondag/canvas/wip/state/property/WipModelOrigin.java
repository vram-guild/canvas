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

/**
 * Describes how vertex coordinates relate to world and camera geometry.
 * Currently vertex collectors don't mix so not part of render state
 * but kept as a global indicator to allow for checking and in-shader information.<p>
 *
 * Except as noted below, GL state is always assumed to have the projection
 * matrix set and view matrix set to identity. This is the default matrix
 * state during work render.
 */
public enum WipModelOrigin {
	/**
	 * Vertex coordinates are relative to the camera and include model transformations
	 * as well as camera rotation and translation via MatrixStack.
	 * The GL view matrix will be the identity matrix. (the default state in world render)
	 * Used for most per-frame renders (entities, block entities, etc.)
	 */
	ENTITY,

	/**
	 * Vertex coordinates are relative to the camera and include model translation, scaling
	 * and billboard rotation plus camera translation via matrix stack but not camera rotation.
	 * The GL view matrix will include camera rotation.
	 * Used for particle rendering.
	 */
	PARTICLE,

	/**
	 * Vertex coordinate are raw model coordinates.
	 * Will need a view matrix update per draw.
	 * Currently not used.
	 */
	MODEL,

	/**
	 * Vertex coordinates are relative to a world region and
	 * include all model transformations.
	 * GL view matrix must be updated for both camera rotation and offset.
	 * Used in terrain rendering. Canvas regions may be 16x16 or 256x256.
	 */
	REGION,

	/**
	 * Vertex coordinates are relative to the screen.
	 * Intended for GUI rendering.
	 * Currently not used.
	 */
	SCREEN;


	private static WipModelOrigin current = ENTITY;

	public static WipModelOrigin get() {
		return current;
	}

	public static void set(WipModelOrigin val) {
		assert val != null;
		current = val;
	}
}
