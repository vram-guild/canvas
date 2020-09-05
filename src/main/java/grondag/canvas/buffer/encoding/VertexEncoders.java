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

import static grondag.canvas.buffer.encoding.HdEncoders.HD_TERRAIN_1;
import static grondag.canvas.buffer.encoding.HdEncoders.HD_TERRAIN_2;
import static grondag.canvas.buffer.encoding.HdEncoders.HD_TERRAIN_3;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_BLOCK_1;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_BLOCK_2;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_BLOCK_3;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_ITEM_1;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_ITEM_2;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_ITEM_3;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_TERRAIN_1;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_TERRAIN_2;
import static grondag.canvas.buffer.encoding.VanillaEncoders.VANILLA_TERRAIN_3;
import static grondag.canvas.material.EncodingContext.BLOCK;
import static grondag.canvas.material.EncodingContext.ITEM;
import static grondag.canvas.material.EncodingContext.TERRAIN;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.material.AbstractMeshMaterial;
import grondag.canvas.apiimpl.material.MeshMaterial;
import grondag.canvas.apiimpl.material.MeshMaterialLocator;
import grondag.canvas.material.EncodingContext;
import grondag.canvas.material.MaterialState;
import grondag.canvas.shader.ShaderPass;

public class VertexEncoders {
	/**
	 * Largest possible number of active encoder indices.
	 */
	public static final int ENCODER_KEY_SPACE_SIZE = 2 * MathHelper.smallestEncompassingPowerOfTwo(AbstractMeshMaterial.MAX_SPRITE_DEPTH)
			* MathHelper.smallestEncompassingPowerOfTwo(EncodingContext.values().length);

	private static final int CONTEXT_SHIFT = Integer.bitCount(MathHelper.smallestEncompassingPowerOfTwo(AbstractMeshMaterial.MAX_SPRITE_DEPTH) - 1);
	private static final int TRANSLUCENT_FLAG = ENCODER_KEY_SPACE_SIZE / 2;

	private static VertexEncoder[] ENCODERS = new VertexEncoder[ENCODER_KEY_SPACE_SIZE];

	static {
		reload();
	}

	/**
	 * Not related to encoder index.
	 */
	private static final int lookupIndex(EncodingContext context, int spriteDepth, boolean isTranslucent) {
		return isTranslucent  ? (TRANSLUCENT_FLAG | (context.ordinal() << CONTEXT_SHIFT) | spriteDepth) : ((context.ordinal() << CONTEXT_SHIFT) | spriteDepth);
	}

	public static VertexEncoder get(EncodingContext context, MeshMaterialLocator matLocator) {
		final MeshMaterial mat = matLocator.get();
		return ENCODERS[lookupIndex(context, mat.spriteDepth(), mat.isTranslucent)];
	}

	public static VertexEncoder getDefault(EncodingContext context, boolean isTranslucent) {
		return ENCODERS[lookupIndex(context, 1, isTranslucent)];
	}

	public static VertexEncoder getDefault(EncodingContext context, MaterialState materialState) {
		return getDefault(context, materialState.shaderPass == ShaderPass.TRANSLUCENT);
	}

	public static void reload() {
		ENCODERS[lookupIndex(BLOCK, 1, false)] = VANILLA_BLOCK_1;
		ENCODERS[lookupIndex(BLOCK, 2, false)] = VANILLA_BLOCK_2;
		ENCODERS[lookupIndex(BLOCK, 3, false)] = VANILLA_BLOCK_3;
		ENCODERS[lookupIndex(BLOCK, 1, true)] = VANILLA_BLOCK_1;
		ENCODERS[lookupIndex(BLOCK, 2, true)] = VANILLA_BLOCK_2;
		ENCODERS[lookupIndex(BLOCK, 3, true)] = VANILLA_BLOCK_3;

		ENCODERS[lookupIndex(TERRAIN, 1, false)] = Configurator.hdLightmaps() ? HD_TERRAIN_1 : VANILLA_TERRAIN_1;
		ENCODERS[lookupIndex(TERRAIN, 2, false)] = Configurator.hdLightmaps() ? HD_TERRAIN_2 : VANILLA_TERRAIN_2;
		ENCODERS[lookupIndex(TERRAIN, 3, false)] = Configurator.hdLightmaps() ? HD_TERRAIN_3 : VANILLA_TERRAIN_3;
		ENCODERS[lookupIndex(TERRAIN, 1, true)] = Configurator.hdLightmaps() ? HD_TERRAIN_1 : VANILLA_TERRAIN_1;
		ENCODERS[lookupIndex(TERRAIN, 2, true)] = Configurator.hdLightmaps() ? HD_TERRAIN_2 : VANILLA_TERRAIN_2;
		ENCODERS[lookupIndex(TERRAIN, 3, true)] = Configurator.hdLightmaps() ? HD_TERRAIN_3 : VANILLA_TERRAIN_3;

		ENCODERS[lookupIndex(ITEM, 1, false)] = VANILLA_ITEM_1;
		ENCODERS[lookupIndex(ITEM, 2, false)] = VANILLA_ITEM_2;
		ENCODERS[lookupIndex(ITEM, 3, false)] = VANILLA_ITEM_3;
		ENCODERS[lookupIndex(ITEM, 1, true)] = VANILLA_ITEM_1;
		ENCODERS[lookupIndex(ITEM, 2, true)] = VANILLA_ITEM_2;
		ENCODERS[lookupIndex(ITEM, 3, true)] = VANILLA_ITEM_3;
	}
}
