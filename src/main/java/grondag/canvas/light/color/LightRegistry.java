package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.light.ItemLight;
import io.vram.frex.base.renderer.util.ResourceCache;

import grondag.canvas.CanvasMod;
import grondag.canvas.light.color.LightRegionData.Encoding;
import grondag.canvas.light.api.impl.BlockLightLoader;

class LightRegistry {
	private static final ResourceCache<Object2ShortOpenHashMap<BlockState>> cachedLights = new ResourceCache<>(Object2ShortOpenHashMap::new);

	public static short get(BlockState blockState){
		return cachedLights.getOrLoad().computeIfAbsent(blockState, LightRegistry::generate);
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
