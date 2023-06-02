package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.light.ItemLight;

public class LightRegistry {
	private static final Int2ShortOpenHashMap lights = new Int2ShortOpenHashMap();

	static {
		lights.defaultReturnValue((short) 0);
	}

	public static short get(BlockState blockState){
		final int stateKey = blockState.hashCode();
		short light = lights.get(stateKey);

		if (light == 0) {
			// PERF: modify ItemLight API or make new API that doesn't need ItemStack
			// TODO: ItemLight API isn't suitable for this anyway since we rely on blockstates not blocks/items
			final ItemStack stack = new ItemStack(blockState.getBlock(), 1);
			final ItemLight itemLight = ItemLight.get(stack);
			final int lightEmission = blockState.getLightEmission();
			final boolean occluding = blockState.canOcclude();

			if (itemLight != null) {
				final int r = (int) (lightEmission * itemLight.red());
				final int g = (int) (lightEmission * itemLight.green());
				final int b = (int) (lightEmission * itemLight.blue());
				light = LightSectionData.Encoding.encodeLight(r, g, b, true, occluding);
			} else {
				light = LightSectionData.Encoding.encodeLight(lightEmission, lightEmission, lightEmission, true, occluding);
			}

			lights.put(stateKey, light);
		}

		return light;
	}
}
