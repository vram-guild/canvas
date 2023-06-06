package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.light.ItemLight;
import io.vram.frex.base.renderer.util.ResourceCache;

public class LightRegistry {
	private static final ResourceCache<Int2ShortOpenHashMap> lights = new ResourceCache<>(() -> {
		final var newMap = new Int2ShortOpenHashMap();
		newMap.defaultReturnValue((short) 0);
		return newMap;
	});

	public static short get(BlockState blockState){
		final int stateKey = blockState.hashCode();
		short light = lights.getOrLoad().get(stateKey);

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
				light = LightRegionData.Encoding.encodeLight(r, g, b, true, occluding);
			} else {
				light = LightRegionData.Encoding.encodeLight(lightEmission, lightEmission, lightEmission, true, occluding);
			}

			lights.getOrLoad().put(stateKey, light);
		}

		return light;
	}
}
