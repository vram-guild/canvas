package grondag.canvas.buffer.encoding;

import static grondag.canvas.material.MaterialContext.BLOCK;
import static grondag.canvas.material.MaterialContext.ITEM_FIXED;
import static grondag.canvas.material.MaterialContext.ITEM_GROUND;
import static grondag.canvas.material.MaterialContext.ITEM_GUI;
import static grondag.canvas.material.MaterialContext.ITEM_HELD;
import static grondag.canvas.material.MaterialContext.TERRAIN;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.material.MaterialContext;

public class VertexEncoders {
	/**
	 * Largest possible number of active encoder indices.  Actual will generally be much fewer.
	 */
	public static final int ENCODER_KEY_SPACE_SIZE = 2 * MathHelper.smallestEncompassingPowerOfTwo(RenderMaterialImpl.MAX_SPRITE_DEPTH)
			* MathHelper.smallestEncompassingPowerOfTwo(MaterialContext.values().length);

	private static final int CONTEXT_SHIFT = Integer.bitCount(MathHelper.smallestEncompassingPowerOfTwo(RenderMaterialImpl.MAX_SPRITE_DEPTH) - 1);
	private static final int TRANSLUCENT_FLAG = ENCODER_KEY_SPACE_SIZE / 2;

	private static VertexEncoder[] ENCODERS = new VertexEncoder[ENCODER_KEY_SPACE_SIZE];

	static {
		reload();
	}

	/**
	 * Not related to encoder index.
	 */
	private static final int lookupIndex(MaterialContext context, int spriteDepth, boolean isTranslucent) {
		return isTranslucent  ? (TRANSLUCENT_FLAG | (context.ordinal() << CONTEXT_SHIFT) | spriteDepth) : ((context.ordinal() << CONTEXT_SHIFT) | spriteDepth);
	}

	private static final VanillaBlockEncoder VANILLA_BLOCK = new VanillaBlockEncoder(0);
	private static final VanillaItemEncoder VANILLA_ITEM = new VanillaItemEncoder(1);
	private static final VanillaTerrainEncoder VANILLA_TERRAIN = new VanillaTerrainEncoder(2);

	public static VertexEncoder get(MaterialContext context, Value mat) {
		return ENCODERS[lookupIndex(context, mat.spriteDepth(), mat.isTranslucent)];
	}

	public static void reload() {
		ENCODERS[lookupIndex(BLOCK, 1, false)] = VANILLA_BLOCK;
		ENCODERS[lookupIndex(BLOCK, 2, false)] = VANILLA_BLOCK;
		ENCODERS[lookupIndex(BLOCK, 3, false)] = VANILLA_BLOCK;
		ENCODERS[lookupIndex(BLOCK, 1, true)] = VANILLA_BLOCK;
		ENCODERS[lookupIndex(BLOCK, 2, true)] = VANILLA_BLOCK;
		ENCODERS[lookupIndex(BLOCK, 3, true)] = VANILLA_BLOCK;

		ENCODERS[lookupIndex(TERRAIN, 1, false)] = VANILLA_TERRAIN;
		ENCODERS[lookupIndex(TERRAIN, 2, false)] = VANILLA_TERRAIN;
		ENCODERS[lookupIndex(TERRAIN, 3, false)] = VANILLA_TERRAIN;
		ENCODERS[lookupIndex(TERRAIN, 1, true)] = VANILLA_TERRAIN;
		ENCODERS[lookupIndex(TERRAIN, 2, true)] = VANILLA_TERRAIN;
		ENCODERS[lookupIndex(TERRAIN, 3, true)] = VANILLA_TERRAIN;

		ENCODERS[lookupIndex(ITEM_HELD, 1, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_HELD, 2, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_HELD, 3, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_HELD, 1, true)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_HELD, 2, true)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_HELD, 3, true)] = VANILLA_ITEM;

		ENCODERS[lookupIndex(ITEM_GUI, 1, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GUI, 2, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GUI, 3, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GUI, 1, true)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GUI, 2, true)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GUI, 3, true)] = VANILLA_ITEM;

		ENCODERS[lookupIndex(ITEM_GROUND, 1, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GROUND, 2, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GROUND, 3, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GROUND, 1, true)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GROUND, 2, true)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_GROUND, 3, true)] = VANILLA_ITEM;

		ENCODERS[lookupIndex(ITEM_FIXED, 1, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_FIXED, 2, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_FIXED, 3, false)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_FIXED, 1, true)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_FIXED, 2, true)] = VANILLA_ITEM;
		ENCODERS[lookupIndex(ITEM_FIXED, 3, true)] = VANILLA_ITEM;
	}
}
