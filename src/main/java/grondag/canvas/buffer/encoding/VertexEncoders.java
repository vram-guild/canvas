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

package grondag.canvas.buffer.encoding;

import grondag.canvas.Configurator;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.state.RenderMaterialImpl;

import static grondag.canvas.buffer.encoding.EncodingContext.BLOCK;
import static grondag.canvas.buffer.encoding.EncodingContext.ITEM;
import static grondag.canvas.buffer.encoding.EncodingContext.TERRAIN;
import static grondag.canvas.buffer.encoding.HdEncoders.HD_TERRAIN_1;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_BLOCK;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_ITEM;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_TERRAIN;

import net.minecraft.util.math.MathHelper;

public class VertexEncoders {
	/**
	 * Largest possible number of active encoder indices.
	 */
	public static final int ENCODER_KEY_SPACE_SIZE = 2 * MathHelper.smallestEncompassingPowerOfTwo(EncodingContext.values().length);
	private static final int CONTEXT_SHIFT = 1;
	private static final int TRANSLUCENT_FLAG = 1;

	private static final VertexEncoder[] ENCODERS = new VertexEncoder[ENCODER_KEY_SPACE_SIZE];

	static {
		reload();
	}

	/**
	 * Not related to encoder index.
	 */
	private static final int lookupIndex(EncodingContext context, boolean isTranslucent) {
		return (isTranslucent ? TRANSLUCENT_FLAG : 0) | (context.ordinal() << CONTEXT_SHIFT);
	}

	public static VertexEncoder get(EncodingContext context, RenderMaterialImpl mat) {
		return ENCODERS[lookupIndex(context, mat.transparency == MaterialTransparency.TRANSLUCENT)];
	}

	public static VertexEncoder getDefault(EncodingContext context, boolean isTranslucent) {
		return ENCODERS[lookupIndex(context, isTranslucent)];
	}

	public static VertexEncoder getDefault(EncodingContext context, RenderMaterialImpl materialState) {
		return getDefault(context, materialState.transparency == MaterialTransparency.TRANSLUCENT);
	}

	public static void reload() {
		ENCODERS[lookupIndex(BLOCK, false)] = VANILLA_BLOCK;
		ENCODERS[lookupIndex(BLOCK, true)] = VANILLA_BLOCK;

		ENCODERS[lookupIndex(TERRAIN, false)] = Configurator.hdLightmaps() ? HD_TERRAIN_1 : VANILLA_TERRAIN;
		ENCODERS[lookupIndex(TERRAIN, true)] = Configurator.hdLightmaps() ? HD_TERRAIN_1 : VANILLA_TERRAIN;

		ENCODERS[lookupIndex(ITEM, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM, true)] = VANILLA_ITEM;
	}
}
