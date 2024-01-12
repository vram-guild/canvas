/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import grondag.canvas.CanvasMod;

public class BlockLightLoader {
	public static final FloodFillBlockLight DEFAULT_LIGHT = new FloodFillBlockLight(0f, 0f, 0f, 0f, false);

	public static final IdentityHashMap<BlockState, FloodFillBlockLight> BLOCK_LIGHTS = new IdentityHashMap<>();
	public static final IdentityHashMap<FluidState, FloodFillBlockLight> FLUID_LIGHTS = new IdentityHashMap<>();
	public static final IdentityHashMap<EntityType<?>, FloodFillBlockLight> ENTITY_LIGHTS = new IdentityHashMap<>();

	public static void reload(ResourceManager manager) {
		BLOCK_LIGHTS.clear();
		FLUID_LIGHTS.clear();
		ENTITY_LIGHTS.clear();

		for (Block block : BuiltInRegistries.BLOCK) {
			loadBlock(manager, block);
		}

		for (Fluid fluid : BuiltInRegistries.FLUID) {
			loadFluid(manager, fluid);
		}

		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			loadEntity(manager, type);
		}
	}

	private static void loadBlock(ResourceManager manager, Block block) {
		final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

		final ResourceLocation id = new ResourceLocation(blockId.getNamespace(), "lights/block/" + blockId.getPath() + ".json");

		try {
			final var res = manager.getResource(id);

			if (res.isPresent()) {
				deserialize(block.getStateDefinition().getPossibleStates(), id, new InputStreamReader(res.get().open(), StandardCharsets.UTF_8), BLOCK_LIGHTS);
			}
		} catch (final Exception e) {
			CanvasMod.LOG.info("Unable to load block light map " + id.toString() + " due to exception " + e.toString());
		}
	}

	private static void loadFluid(ResourceManager manager, Fluid fluid) {
		final ResourceLocation blockId = BuiltInRegistries.FLUID.getKey(fluid);

		final ResourceLocation id = new ResourceLocation(blockId.getNamespace(), "lights/fluid/" + blockId.getPath() + ".json");

		try {
			final var res = manager.getResource(id);

			if (res.isPresent()) {
				deserialize(fluid.getStateDefinition().getPossibleStates(), id, new InputStreamReader(res.get().open(), StandardCharsets.UTF_8), FLUID_LIGHTS);
			}
		} catch (final Exception e) {
			CanvasMod.LOG.info("Unable to load fluid light map " + id.toString() + " due to exception " + e.toString());
		}
	}

	private static void loadEntity(ResourceManager manager, EntityType<?> entityType) {
		final ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
		final ResourceLocation id = new ResourceLocation(entityId.getNamespace(), "lights/entity/" + entityId.getPath() + ".json");

		try {
			final var res = manager.getResource(id);

			if (res.isPresent()) {
				deserialize(entityType, id, new InputStreamReader(res.get().open(), StandardCharsets.UTF_8), ENTITY_LIGHTS);
			}
		} catch (final Exception e) {
			CanvasMod.LOG.info("Unable to load block light map " + id.toString() + " due to exception " + e.toString());
		}
	}

	private static <T extends StateHolder<?, ?>> void deserialize(List<T> states, ResourceLocation idForLog, InputStreamReader reader, IdentityHashMap<T, FloodFillBlockLight> map) {
		try {
			final JsonObject json = GsonHelper.parse(reader);
			final String idString = idForLog.toString();

			final FloodFillBlockLight globalDefaultLight = DEFAULT_LIGHT;
			final FloodFillBlockLight defaultLight;

			if (json.has("defaultLight")) {
				defaultLight = loadLight(json.get("defaultLight").getAsJsonObject(), globalDefaultLight);
			} else {
				defaultLight = globalDefaultLight;
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
				FloodFillBlockLight result = defaultLight;

				if (!result.levelIsSet && state instanceof BlockState blockState) {
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

	private static <T> void deserialize(T type, ResourceLocation idForLog, InputStreamReader reader, IdentityHashMap<T, FloodFillBlockLight> map) {
		try {
			final JsonObject json = GsonHelper.parse(reader);
			final FloodFillBlockLight globalDefaultLight = BlockLightLoader.DEFAULT_LIGHT;
			final FloodFillBlockLight result;

			if (json.has("defaultLight")) {
				result = BlockLightLoader.loadLight(json.get("defaultLight").getAsJsonObject(), globalDefaultLight);
			} else {
				result = BlockLightLoader.loadLight(json, globalDefaultLight);
			}

			if (!result.equals(globalDefaultLight) && result.levelIsSet) {
				map.put(type, result);
			}
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to load lights for " + idForLog.toString() + " due to unhandled exception:", e);
		}
	}

	private static FloodFillBlockLight loadLight(JsonObject obj, FloodFillBlockLight defaultValue) {
		if (obj == null) {
			return defaultValue;
		}

		final var lightLevelObj = obj.get("lightLevel");
		final var redObj = obj.get("red");
		final var greenObj = obj.get("green");
		final var blueObj = obj.get("blue");

		final float defaultLightLevel = defaultValue.levelIsSet ? defaultValue.lightLevel() : 15f;
		final float lightLevel = lightLevelObj == null ? defaultLightLevel : lightLevelObj.getAsFloat();
		final float red = redObj == null ? defaultValue.red() : redObj.getAsFloat();
		final float green = greenObj == null ? defaultValue.green() : greenObj.getAsFloat();
		final float blue = blueObj == null ? defaultValue.blue() : blueObj.getAsFloat();
		final boolean levelIsSet = lightLevelObj != null || defaultValue.levelIsSet;
		final var result = new FloodFillBlockLight(lightLevel, red, green, blue, levelIsSet);

		if (result.equals(defaultValue)) {
			return defaultValue;
		} else {
			return result;
		}
	}
}
