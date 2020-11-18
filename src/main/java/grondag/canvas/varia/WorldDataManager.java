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

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.frex.api.light.ItemLight;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class WorldDataManager {
	public static final int LENGTH = 22;

	private static final int WORLD_EFFECT_MODIFIER = 0;
	private static final int RENDER_SECONDS = 1;
	private static final int AMBIENT_INTENSITY = 2;
	private static final int MOON_SIZE = 3;
	private static final int WORLD_TIME = 4;
	private static final int WORLD_DAYS = 5;
	private static final int FLAGS_0 = 6;
	@SuppressWarnings("unused") // was previously used for fog
	private static final int RESERVED = 7;
	private static final int EMISSIVE_COLOR_RED = 8;
	private static final int EMISSIVE_COLOR_GREEN = 9;
	private static final int EMISSIVE_COLOR_BLUE = 10;
	private static final int HELD_LIGHT_RED = 11;
	private static final int HELD_LIGHT_GREEN = 12;
	private static final int HELD_LIGHT_BLUE = 13;
	private static final int HELD_LIGHT_INTENSITY = 14;
	private static final int RAIN_STRENGTH = 15;
	private static final int CAMERA_VIEW = 16; // 3 elements wide
	private static final int ENTITY_VIEW = 19; // 3 elements wide

	private static final int FLAG0_NIGHT_VISTION_ACTIVE = 1;
	private static final int FLAG0_HAS_SKYLIGHT = 2;
	private static final int FLAG0_IS_OVERWORLD = 4;
	private static final int FLAG0_IS_NETHER = 8;
	private static final int FLAG0_IS_END = 16;
	private static final int FLAG0_IS_RAINING = 32;
	private static final int FLAG0_IS_THUNDERING = 64;
	private static final int FLAG0_IS_SKY_DARKENED = 128;

	private static final float[] DATA = new float[LENGTH];
	private static final long baseRenderTime = System.currentTimeMillis();

	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: WorldDataManager static init");
		}
	}

	public static float[] data() {
		return DATA;
	}

	/**
	 * Called just before terrain setup each frame after camera, fog and projection
	 * matrix are set up,
	 */
	public static void update(Camera camera) {
		final MinecraftClient client = MinecraftClient.getInstance();
		final Entity cameraEntity = camera.getFocusedEntity();
		final float tickDelta = client.getTickDelta();
		assert cameraEntity != null;
		assert cameraEntity.getEntityWorld() != null;

		if (cameraEntity == null || cameraEntity.getEntityWorld() == null) {
			return;
		}

		DATA[RENDER_SECONDS] = (System.currentTimeMillis() - baseRenderTime) / 1000f;

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
				flags |= FLAG0_IS_RAINING;
			}

			if (world.getSkyProperties().isDarkened()) {
				flags |= FLAG0_IS_SKY_DARKENED;
			}

			if (world.isThundering()) {
				flags |= FLAG0_IS_THUNDERING;
			}

			DATA[FLAGS_0] = flags;

			DATA[RAIN_STRENGTH] = world.getRainGradient(tickDelta);

			ItemLight light = ItemLight.NONE;

			if (player != null)  {
				light = ItemLight.get(player.getMainHandStack());

				if (light == ItemLight.NONE) {
					light = ItemLight.get(player.getOffHandStack());
				}

				if (!light.worksInFluid() && player.isInsideWaterOrBubbleColumn()) {
					light = ItemLight.NONE;
				}
			}

			DATA[HELD_LIGHT_RED] = light.red();
			DATA[HELD_LIGHT_GREEN] = light.green();
			DATA[HELD_LIGHT_BLUE] = light.blue();
			DATA[HELD_LIGHT_INTENSITY] = light.intensity();

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
		}

		putViewVector(CAMERA_VIEW, camera.getYaw(), camera.getPitch());
		putViewVector(ENTITY_VIEW, cameraEntity.yaw, cameraEntity.pitch);
	}

	public static void updateEmissiveColor(int color) {
		DATA[EMISSIVE_COLOR_RED] = ((color >> 24) & 0xFF) / 255f;
		DATA[EMISSIVE_COLOR_GREEN] = ((color >> 16) & 0xFF) / 255f;
		DATA[EMISSIVE_COLOR_BLUE] = (color & 0xFF) / 255f;
	}

	private static void putViewVector(int index, float yaw, float pitch) {
		final float y = (float) Math.toRadians(yaw);
		final float p = (float) Math.toRadians(pitch);

		DATA[index] = -MathHelper.sin(y) * MathHelper.cos(p);
		DATA[index + 1] = -MathHelper.sin(p);
		DATA[index + 2] = MathHelper.cos(y) * MathHelper.cos(p);
	}
}
