package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.light.ItemLight;
import io.vram.frex.base.renderer.util.ResourceCache;

import grondag.canvas.light.color.LightRegionData.Encoding;

public class LightRegistry {
	private static final ResourceCache<Object2ShortOpenHashMap<BlockState>> cachedLights = new ResourceCache<>(() -> {
		final var newMap = new Object2ShortOpenHashMap<BlockState>();
		newMap.defaultReturnValue((short) 0);
		return newMap;
	});

	public static short get(BlockState blockState){
		if (BlockLightRegistry.INSTANCE == null) {
			// TODO: WIP: remove this for production and fail silently..
			throw new IllegalStateException("BlockLightRegistry is unloaded");
		}

		BlockLightRegistry.CachedBlockLight getLight = BlockLightRegistry.INSTANCE.blockLights.get(blockState);

		if (getLight == null && !blockState.getFluidState().isEmpty()) {
			getLight = BlockLightRegistry.INSTANCE.fluidLights.get(blockState.getFluidState());
		}

		if (getLight != null) {
			return getLight.value();
		} else if (blockState.getLightEmission() <= 0) {
			return Encoding.encodeLight(0, false, blockState.canOcclude());
		}

		// CanvasMod.LOG.info("Can't find cached light for block state " + blockState);

		// Item Light color-only fallback (feature?)

		if (!cachedLights.getOrLoad().containsKey(blockState)) {
			final ItemStack stack = new ItemStack(blockState.getBlock(), 1);
			final ItemLight itemLight = ItemLight.get(stack);
			final int lightLevel = blockState.getLightEmission();
			final boolean occluding = blockState.canOcclude();
			final short defaultLight = Encoding.encodeLight(lightLevel, lightLevel, lightLevel, true, occluding);

			if (itemLight == null) {
				cachedLights.getOrLoad().put(blockState, defaultLight);
				return defaultLight;
			}

			float maxValue = Math.max(itemLight.red(), Math.max(itemLight.green(), itemLight.blue()));

			if (maxValue <= 0) {
				cachedLights.getOrLoad().put(blockState, defaultLight);
				return defaultLight;
			}

			final int r = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.red() / maxValue));
			final int g = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.green() / maxValue));
			final int b = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.blue() / maxValue));

			cachedLights.getOrLoad().put(blockState, Encoding.encodeLight(r, g, b, true, occluding));
		}

		return cachedLights.getOrLoad().getShort(blockState);
	}
}
