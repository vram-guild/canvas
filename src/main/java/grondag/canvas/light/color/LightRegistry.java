package grondag.canvas.light.color;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.light.ItemLight;

import grondag.canvas.light.color.LightRegionData.Encoding;
import grondag.canvas.light.api.impl.BlockLightLoader;

public class LightRegistry {
	private static final ConcurrentHashMap<BlockState, Short> cachedLights = new ConcurrentHashMap<>();

	public static void reload(ResourceManager manager) {
		cachedLights.clear();
		BlockLightLoader.reload(manager);
	}

	public static short get(BlockState blockState){
		return cachedLights.computeIfAbsent(blockState, LightRegistry::generate);
	}

	private static short generate(BlockState blockState) {
		final int lightLevel = blockState.getLightEmission();
		final short defaultLight = Encoding.encodeLight(lightLevel, lightLevel, lightLevel, lightLevel > 0, blockState.canOcclude());

		BlockLightLoader.CachedBlockLight apiLight = BlockLightLoader.INSTANCE.blockLights.get(blockState);

		if (apiLight == null && !blockState.getFluidState().isEmpty()) {
			apiLight = BlockLightLoader.INSTANCE.fluidLights.get(blockState.getFluidState());
		}

		if (apiLight != null) {
			return Encoding.encodeLight(apiLight.value(), apiLight.value() != 0, blockState.canOcclude());
		}

		if (lightLevel < 1) {
			return defaultLight;
		}

		// Item Light color-only fallback (feature?)
		final ItemStack stack = new ItemStack(blockState.getBlock(), 1);
		final ItemLight itemLight = ItemLight.get(stack);

		if (itemLight == null) {
			return defaultLight;
		}

		float maxValue = Math.max(itemLight.red(), Math.max(itemLight.green(), itemLight.blue()));

		if (maxValue <= 0) {
			return defaultLight;
		}

		final int r = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.red() / maxValue));
		final int g = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.green() / maxValue));
		final int b = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.blue() / maxValue));

		return Encoding.encodeLight(r, g, b, true, blockState.canOcclude());
	}
}
