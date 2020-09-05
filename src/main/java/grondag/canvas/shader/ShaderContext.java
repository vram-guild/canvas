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

package grondag.canvas.shader;

import grondag.canvas.material.EncodingContext;

public class ShaderContext {
	public static final ShaderContext TERRAIN_SOLID = builder()
			.pass(ShaderPass.SOLID)
			.materialContext(EncodingContext.TERRAIN)
			.build();
	public static final ShaderContext TERRAIN_DECAL = builder()
			.pass(ShaderPass.DECAL)
			.materialContext(EncodingContext.TERRAIN)
			.build();
	public static final ShaderContext TERRAIN_TRANSLUCENT = builder()
			.pass(ShaderPass.TRANSLUCENT)
			.materialContext(EncodingContext.TERRAIN)
			.build();
	public static final ShaderContext PROCESS = builder()
			.pass(ShaderPass.PROCESS)
			.materialContext(EncodingContext.PROCESS)
			.build();
	public static final ShaderContext ENTITY_BLOCK_SOLID = builder()
			.pass(ShaderPass.SOLID)
			.materialContext(EncodingContext.ENTITY_BLOCK)
			.build();
	private static int indexCounter;
	public final int index = ++indexCounter;
	public final EncodingContext materialContext;
	public final ShaderPass pass;
	public final String name;

	private ShaderContext(Builder builder) {
		materialContext = builder.materialContext;
		pass = builder.pass;
		name = materialContext == EncodingContext.PROCESS && pass == ShaderPass.PROCESS ? "process" : materialContext.name().toLowerCase() + "-" + pass.name().toLowerCase();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private EncodingContext materialContext;
		private ShaderPass pass = ShaderPass.SOLID;

		Builder materialContext(EncodingContext materialContext) {
			this.materialContext = materialContext;
			return this;
		}

		Builder pass(ShaderPass pass) {
			this.pass = pass == null ? ShaderPass.SOLID : pass;
			return this;
		}

		ShaderContext build() {
			return new ShaderContext(this);
		}
	}
}
