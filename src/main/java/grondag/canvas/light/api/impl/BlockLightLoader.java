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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import grondag.canvas.CanvasMod;
import grondag.canvas.light.api.BlockLight;
import grondag.canvas.light.color.LightOp;

public class BlockLightLoader {
	public static BlockLightLoader INSTANCE;
	private static final CachedBlockLight DEFAULT_LIGHT = new CachedBlockLight(0f, 0f, 0f, 0f, false);

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
				CachedBlockLight result = defaultLight;

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

	public static CachedBlockLight loadLight(JsonObject obj, CachedBlockLight defaultValue) {
		if (obj == null) {
			return defaultValue;
		}

		final var lightLevelObj = obj.get("lightLevel");
		final var redObj = obj.get("red");
		final var greenObj = obj.get("green");
		final var blueObj = obj.get("blue");

		final float lightLevel = lightLevelObj == null ? defaultValue.lightLevel() : lightLevelObj.getAsFloat();
		final float red = redObj == null ? defaultValue.red() : redObj.getAsFloat();
		final float green = greenObj == null ? defaultValue.green() : greenObj.getAsFloat();
		final float blue = blueObj == null ? defaultValue.blue() : blueObj.getAsFloat();
		final boolean levelIsSet = lightLevelObj == null ? defaultValue.levelIsSet() : true;
		final var result = new CachedBlockLight(lightLevel, red, green, blue, levelIsSet);

		if (result.equals(defaultValue)) {
			return defaultValue;
		} else {
			return result;
		}
	}

	private static int clampLight(float light) {
		return org.joml.Math.clamp(0, 15, Math.round(light));
	}

	public static record CachedBlockLight(float lightLevel, float red, float green, float blue, short value, boolean levelIsSet) implements BlockLight {
		CachedBlockLight(float lightLevel, float red, float green, float blue, boolean levelIsSet) {
			this(lightLevel, red, green, blue, computeValue(lightLevel, red, green, blue), levelIsSet);
		}

		public CachedBlockLight withLevel(float lightEmission) {
			if (this.lightLevel == lightEmission && this.levelIsSet) {
				return this;
			} else {
				return new CachedBlockLight(lightEmission, red, green, blue, true);
			}
		}

		static short computeValue(float lightLevel, float red, float green, float blue) {
			final int blockRadius = lightLevel == 0f ? 0 : org.joml.Math.clamp(1, 15, Math.round(lightLevel));
			return LightOp.encode(clampLight(blockRadius * red), clampLight(blockRadius * green), clampLight(blockRadius * blue), 0);
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
