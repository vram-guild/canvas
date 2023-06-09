package grondag.canvas.light.api.impl;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import grondag.canvas.CanvasMod;
import grondag.canvas.light.api.BlockLight;
import grondag.canvas.light.color.Elem;

public class BlockLightLoader {
	public static BlockLightLoader INSTANCE;
	private static final CachedBlockLight DEFAULT_LIGHT = new CachedBlockLight(0f, 0f, 0f, 0f);

	public final IdentityHashMap<BlockState, CachedBlockLight> blockLights = new IdentityHashMap<>();
	public final IdentityHashMap<FluidState, CachedBlockLight> fluidLights = new IdentityHashMap<>();

	public static void reload(ResourceManager manager) {
		INSTANCE = new BlockLightLoader();

		for (Block block : BuiltInRegistries.BLOCK) {
			INSTANCE.loadBlock(manager, block);
		}

		for (Fluid fluid : BuiltInRegistries.FLUID) {
			INSTANCE.loadFluid(manager, fluid);
		}
	}

	private void loadBlock(ResourceManager manager, Block block) {
		final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

		final ResourceLocation id = new ResourceLocation(blockId.getNamespace(), "lights/block/" + blockId.getPath() + ".json");

		try {
			final var res = manager.getResource(id);

			if (res.isPresent()) {
				deserialize(block.getStateDefinition().getPossibleStates(), id, new InputStreamReader(res.get().open(), StandardCharsets.UTF_8), blockLights);
			}
		} catch (final Exception e) {
			CanvasMod.LOG.info("Unable to load block light map " + id.toString() + " due to exception " + e.toString());
		}
	}

	private void loadFluid(ResourceManager manager, Fluid fluid) {
		final ResourceLocation blockId = BuiltInRegistries.FLUID.getKey(fluid);

		final ResourceLocation id = new ResourceLocation(blockId.getNamespace(), "lights/fluid/" + blockId.getPath() + ".json");

		try {
			final var res = manager.getResource(id);

			if (res.isPresent()) {
				deserialize(fluid.getStateDefinition().getPossibleStates(), id, new InputStreamReader(res.get().open(), StandardCharsets.UTF_8), fluidLights);
			}
		} catch (final Exception e) {
			CanvasMod.LOG.info("Unable to load fluid light map " + id.toString() + " due to exception " + e.toString());
		}
	}

	public static <T extends StateHolder<?, ?>> void deserialize(List<T> states, ResourceLocation idForLog, InputStreamReader reader, IdentityHashMap<T, CachedBlockLight> map) {
		try {
			final JsonObject json = GsonHelper.parse(reader);
			final String idString = idForLog.toString();

			final CachedBlockLight globalDefaultLight = DEFAULT_LIGHT;
			final CachedBlockLight defaultLight;
			final boolean levelIsUnset;

			if (json.has("defaultLight")) {
				defaultLight = loadLight(json.get("defaultLight").getAsJsonObject(), globalDefaultLight);
				levelIsUnset = !json.get("defaultLight").getAsJsonObject().has("radius");
			} else {
				defaultLight = globalDefaultLight;
				levelIsUnset = true;
			}

			JsonObject variants = null;

			if (json.has("variants")) {
				variants = json.getAsJsonObject("variants");

				if (variants.isJsonNull()) {
					CanvasMod.LOG.warn("Unable to load variant lights for " + idString + " because the 'variants' block is empty. Using default map.");
					variants = null;
				}
			}

			for (final T state : states) {
				CachedBlockLight result = defaultLight;

				if (levelIsUnset && state instanceof BlockState blockState) {
					result = result.withLevel(blockState.getLightEmission());
				}

				if (variants != null) {
					final String stateId = BlockModelShaper.statePropertiesToString(state.getValues());
					result = loadLight(variants.getAsJsonObject(stateId), result);
				}

				if (!result.equals(globalDefaultLight)) {
					map.put(state, result);
				}
			}
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to load lights for " + idForLog.toString() + " due to unhandled exception:", e);
		}
	}

	public static CachedBlockLight loadLight(JsonObject obj, CachedBlockLight defaultValue) {
		if (obj == null) {
			return defaultValue;
		}

		final var radiusObj = obj.get("radius");
		final var redObj = obj.get("red");
		final var greenObj = obj.get("green");
		final var blueObj = obj.get("blue");

		final float radius = radiusObj == null ? defaultValue.lightLevel() : radiusObj.getAsFloat();
		final float red = redObj == null ? defaultValue.red() : redObj.getAsFloat();
		final float green = greenObj == null ? defaultValue.green() : greenObj.getAsFloat();
		final float blue = blueObj == null ? defaultValue.blue() : blueObj.getAsFloat();
		final var result = new CachedBlockLight(radius, red, green, blue);

		if (result.equals(defaultValue)) {
			return defaultValue;
		} else {
			return result;
		}
	}

	private static int clampLight(float light) {
		return org.joml.Math.clamp(0, 15, Math.round(light));
	}

	public static record CachedBlockLight(float lightLevel, float red, float green, float blue, short value) implements BlockLight {
		CachedBlockLight(float radius, float red, float green, float blue) {
			this(radius, red, green, blue, computeValue(radius, red, green, blue));
		}

		CachedBlockLight withLevel(float lightEmission) {
			if (this.lightLevel == lightEmission) {
				return this;
			} else {
				return new CachedBlockLight(lightEmission, red, green, blue);
			}
		}

		static short computeValue(float radius, float red, float green, float blue) {
			final int blockRadius = radius == 0f ? 0 : org.joml.Math.clamp(1, 15, Math.round(radius));
			return Elem.encode(clampLight(blockRadius * red), clampLight(blockRadius * green), clampLight(blockRadius * blue), 0);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}

			CachedBlockLight that = (CachedBlockLight) obj;

			if (that.lightLevel != lightLevel) {
				return false;
			}

			if (that.red != red) {
				return false;
			}

			if (that.green != green) {
				return false;
			}

			if (that.blue != blue) {
				return false;
			}

			return value == that.value;
		}
	}
}
