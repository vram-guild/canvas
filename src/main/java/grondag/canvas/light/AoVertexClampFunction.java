/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
 ******************************************************************************/
package grondag.canvas.light;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.Configurator;

@Environment(EnvType.CLIENT)
public final class AoVertexClampFunction {
	@FunctionalInterface
	private interface ClampFunc {
		float clamp(float x);
	}

	static ClampFunc func;

	static {
		reload();
	}

	public static void reload() {
		func = Configurator.clampExteriorVertices ? x -> x < 0f ? 0f : (x > 1f ? 1f : x) : x -> x;
	}

	static float clamp(float x) {
		return func.clamp(x);
	}
}
