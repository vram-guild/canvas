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

package grondag.canvas.shader.data;

/**
 * Governs how often shader uniform initializers are called.
 *
 * <p>In all cases, initializers will only be called if a shader using the uniform
 * is activated and values are only uploaded if they have changed.
 */
public enum UniformRefreshFrequency {
	/**
	 * Uniform initializer only called 1X a time of program load or reload.
	 */
	ON_LOAD,

	/**
	 * Uniform initializer called 1X per game tick. (20X per second)
	 */
	PER_TICK,

	/**
	 * Uniform initializer called 1X per render frame. (Variable frequency.)
	 */
	PER_FRAME
}
