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

package grondag.canvas.varia;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.world.World;

public class WorldDataManager {
	public static final int LENGTH = 16;

	private static final int WORLD_EFFECT_MODIFIER = 0;
	private static final int RENDER_SECONDS = 1;
	private static final int AMBIENT_INTENSITY = 2;
	private static final int MOON_SIZE = 3;
	private static final int WORLD_TIME = 4;
	private static final int WORLD_DAYS = 5;
	private static final int FLAGS_0 = 6;
	private static final int FOG_MODE = 7;
	private static final int EMISSIVE_COLOR_RED = 8;
	private static final int EMISSIVE_COLOR_GREEN = 9;
	private static final int EMISSIVE_COLOR_BLUE = 10;
	private static final int HELD_LIGHT_RED = 11;
	private static final int HELD_LIGHT_GREEN = 12;
	private static final int HELD_LIGHT_BLUE = 13;
	private static final int HELD_LIGHT_INTENSITY = 14;
	private static final int RAIN_STRENGTH = 15;

	// TODO: add player eye position (for 3rd-person views)
	// TODO: add model origin to allow converting to world coordinates - or confirm view coordinates do that
	// update shader docs in that case to make clear vertex coordinates are model, not world
	private static final int FLAG0_NIGHT_VISTION_ACTIVE = 1;
	private static final int FLAG0_HAS_SKYLIGHT = 2;
	private static final int FLAG0_IS_OVERWORLD = 4;
	private static final int FLAG0_IS_NETHER = 8;
	private static final int FLAG0_IS_END = 16;
	private static final int FLAG0_IS_RAINING = 32;
	private static final int FLAG0_IS_THUNDERING = 64;

	private static final float[] DATA = new float[LENGTH];

	public static float[] data() {
		return DATA;
	}

	private static final long baseRenderTime = System.currentTimeMillis();

	public static void update(float tickDelta) {
		DATA[RENDER_SECONDS] = (System.currentTimeMillis() - baseRenderTime) / 1000f;

		final MinecraftClient client = MinecraftClient.getInstance();
		final ClientWorld world = client.world;
		if (world != null) {
			final long days = world.getTimeOfDay() / 24000L;
			DATA[WORLD_DAYS] = (int) (days % 2147483647L);
			DATA[WORLD_TIME] = (float) ((world.getTimeOfDay() - days * 24000L) / 24000.0);
			final ClientPlayerEntity player = client.player;

			int flags = world.getDimension().hasSkyLight() ? FLAG0_HAS_SKYLIGHT : 0;

			final boolean nightVision = player != null && client.player.hasStatusEffect(StatusEffects.NIGHT_VISION);

			if (nightVision) {
				flags |= FLAG0_NIGHT_VISTION_ACTIVE;
			}

			if (world.getRegistryKey() == World.OVERWORLD) {
				flags |= FLAG0_IS_OVERWORLD;
			} else if (world.getRegistryKey() == World.NETHER) {
				flags |= FLAG0_IS_NETHER;
			} else if (world.getRegistryKey() == World.END) {
				flags |= FLAG0_IS_END;
			}

			if (world.isRaining()) {
				flags  |= FLAG0_IS_RAINING;
			}


			if (world.isThundering()) {
				flags |= FLAG0_IS_THUNDERING;
			}

			DATA[FLAGS_0] = flags;

			DATA[RAIN_STRENGTH] = world.getRainGradient(tickDelta);

			// TODO: use item tags
			if (player != null && player.isHolding(Items.TORCH)) {
				DATA[HELD_LIGHT_RED] = 1f;
				DATA[HELD_LIGHT_GREEN] = 1f;
				DATA[HELD_LIGHT_BLUE] = 0.8f;
				DATA[HELD_LIGHT_INTENSITY] = 1f;
			} else  {
				DATA[HELD_LIGHT_RED] = 0f;
				DATA[HELD_LIGHT_GREEN] = 0f;
				DATA[HELD_LIGHT_BLUE] = 0f;
				DATA[HELD_LIGHT_INTENSITY] = 0f;
			}

			DATA[AMBIENT_INTENSITY] = world.method_23783(1.0F);
			DATA[MOON_SIZE] = world.getMoonSize();

			final float fluidModifier = client.player.getUnderwaterVisibility();

			if (nightVision) {
				DATA[WORLD_EFFECT_MODIFIER] = GameRenderer.getNightVisionStrength(client.player, tickDelta);
			} else if (fluidModifier > 0.0F && client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
				DATA[WORLD_EFFECT_MODIFIER] = fluidModifier;
			} else {
				DATA[WORLD_EFFECT_MODIFIER] = 0.0F;
			}

			final int fogMode = FogStateExtHolder.INSTANCE.getMode();

			// Convert to values more reliably read as floats
			if (fogMode == 2048) {
				// EXP
				DATA[FOG_MODE] = 1.0f;
			} else if (fogMode  == 2049) {
				// EXP2
				DATA[FOG_MODE] = 2.0f;
			} else {
				assert fogMode == 9729;
				// LINEAR
				DATA[FOG_MODE] = 0.0f;
			}
		}
	}

	public static void updateEmissiveColor(int color) {
		DATA[EMISSIVE_COLOR_RED] = ((color >> 24) &  0xFF) / 255f;
		DATA[EMISSIVE_COLOR_GREEN] = ((color >> 16) &  0xFF) / 255f;
		DATA[EMISSIVE_COLOR_BLUE] = (color &  0xFF) / 255f;
	}
}
