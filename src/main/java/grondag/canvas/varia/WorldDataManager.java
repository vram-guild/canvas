/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.varia;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.fermion.bits.BitPacker32;
import grondag.frex.api.light.ItemLight;

public class WorldDataManager {
	public static final int LENGTH = 22;

	private static final int WORLD_EFFECT_MODIFIER = 0;
	private static final int RENDER_SECONDS = 1;
	private static final int AMBIENT_INTENSITY = 2;
	private static final int MOON_SIZE = 3;
	private static final int WORLD_TIME = 4;
	private static final int WORLD_DAYS = 5;
	@SuppressWarnings("unused") // was previously used for flags
	private static final int RESERVED_A = 6;
	@SuppressWarnings("unused") // was previously used for fog
	private static final int RESERVED_B = 7;
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

	private static final BitPacker32<Void> WORLD_FLAGS = new BitPacker32<>(null, null);
	private static final BitPacker32<Void>.BooleanElement FLAG_HAS_SKYLIGHT = WORLD_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_IS_OVERWORLD = WORLD_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_IS_NETHER = WORLD_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_IS_END = WORLD_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_IS_RAINING = WORLD_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_IS_THUNDERING = WORLD_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_IS_SKY_DARKENED = WORLD_FLAGS.createBooleanElement();

	private static final BitPacker32<Void> PLAYER_FLAGS = new BitPacker32<>(null, null);
	private static final BitPacker32<Void>.BooleanElement FLAG_SPEED = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_SLOWNESS = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_HASTE = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_MINING_FATIGUE = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_STRENGTH = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_INSTANT_HEALTH = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_INSTANT_DAMAGE = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_JUMP_BOOST = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_NAUSEA = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_REGENERATION = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_RESISTANCE = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_FIRE_RESISTANCE = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_WATER_BREATHING = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_INVISIBILITY = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_BLINDNESS = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_NIGHT_VISION = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_HUNGER = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_WEAKNESS = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_POISON = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_WITHER = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_HEALTH_BOOST = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_ABSORPTION = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_SATURATION = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_GLOWING = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_LEVITATION = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_LUCK = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_UNLUCK = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_SLOW_FALLING = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_CONDUIT_POWER = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_DOLPHINS_GRACE = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_BAD_OMEN = PLAYER_FLAGS.createBooleanElement();
	private static final BitPacker32<Void>.BooleanElement FLAG_HERO_OF_THE_VILLAGE = PLAYER_FLAGS.createBooleanElement();

	public static final FloatBuffer DATA = BufferUtils.createFloatBuffer(LENGTH);
	private static final long baseRenderTime = System.currentTimeMillis();
	private static int worldFlags;
	private static int playerFlags;

	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: WorldDataManager static init");
		}
	}

	/**
	 * Called just before terrain setup each frame after camera, fog and projection
	 * matrix are set up.
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

		DATA.put(RENDER_SECONDS, (System.currentTimeMillis() - baseRenderTime) / 1000f);

		final ClientWorld world = client.world;

		if (world != null) {
			final long days = world.getTimeOfDay() / 24000L;
			DATA.put(WORLD_DAYS, (int) (days % 2147483647L));
			DATA.put(WORLD_TIME, (float) ((world.getTimeOfDay() - days * 24000L) / 24000.0));
			final ClientPlayerEntity player = client.player;

			worldFlags = 0;
			playerFlags = 0;

			worldFlags = FLAG_HAS_SKYLIGHT.setValue(world.getDimension().hasSkyLight(), worldFlags);

			final boolean nightVision = player != null && client.player.hasStatusEffect(StatusEffects.NIGHT_VISION);

			playerFlags = FLAG_SPEED.setValue(client.player.hasStatusEffect(StatusEffects.SPEED), playerFlags);
			playerFlags = FLAG_SLOWNESS.setValue(client.player.hasStatusEffect(StatusEffects.SLOWNESS), playerFlags);
			playerFlags = FLAG_HASTE.setValue(client.player.hasStatusEffect(StatusEffects.HASTE), playerFlags);
			playerFlags = FLAG_MINING_FATIGUE.setValue(client.player.hasStatusEffect(StatusEffects.MINING_FATIGUE), playerFlags);
			playerFlags = FLAG_STRENGTH.setValue(client.player.hasStatusEffect(StatusEffects.STRENGTH), playerFlags);
			playerFlags = FLAG_INSTANT_HEALTH.setValue(client.player.hasStatusEffect(StatusEffects.INSTANT_HEALTH), playerFlags);
			playerFlags = FLAG_INSTANT_DAMAGE.setValue(client.player.hasStatusEffect(StatusEffects.INSTANT_DAMAGE), playerFlags);
			playerFlags = FLAG_JUMP_BOOST.setValue(client.player.hasStatusEffect(StatusEffects.JUMP_BOOST), playerFlags);
			playerFlags = FLAG_NAUSEA.setValue(client.player.hasStatusEffect(StatusEffects.NAUSEA), playerFlags);
			playerFlags = FLAG_REGENERATION.setValue(client.player.hasStatusEffect(StatusEffects.REGENERATION), playerFlags);
			playerFlags = FLAG_RESISTANCE.setValue(client.player.hasStatusEffect(StatusEffects.RESISTANCE), playerFlags);
			playerFlags = FLAG_FIRE_RESISTANCE.setValue(client.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE), playerFlags);
			playerFlags = FLAG_WATER_BREATHING.setValue(client.player.hasStatusEffect(StatusEffects.WATER_BREATHING), playerFlags);
			playerFlags = FLAG_INVISIBILITY.setValue(client.player.hasStatusEffect(StatusEffects.INVISIBILITY), playerFlags);
			playerFlags = FLAG_BLINDNESS.setValue(client.player.hasStatusEffect(StatusEffects.BLINDNESS), playerFlags);
			playerFlags = FLAG_NIGHT_VISION.setValue(nightVision, playerFlags);
			playerFlags = FLAG_HUNGER.setValue(client.player.hasStatusEffect(StatusEffects.HUNGER), playerFlags);
			playerFlags = FLAG_WEAKNESS.setValue(client.player.hasStatusEffect(StatusEffects.WEAKNESS), playerFlags);
			playerFlags = FLAG_POISON.setValue(client.player.hasStatusEffect(StatusEffects.POISON), playerFlags);
			playerFlags = FLAG_WITHER.setValue(client.player.hasStatusEffect(StatusEffects.WITHER), playerFlags);
			playerFlags = FLAG_HEALTH_BOOST.setValue(client.player.hasStatusEffect(StatusEffects.HEALTH_BOOST), playerFlags);
			playerFlags = FLAG_ABSORPTION.setValue(client.player.hasStatusEffect(StatusEffects.ABSORPTION), playerFlags);
			playerFlags = FLAG_SATURATION.setValue(client.player.hasStatusEffect(StatusEffects.SATURATION), playerFlags);
			playerFlags = FLAG_GLOWING.setValue(client.player.hasStatusEffect(StatusEffects.GLOWING), playerFlags);
			playerFlags = FLAG_LEVITATION.setValue(client.player.hasStatusEffect(StatusEffects.LEVITATION), playerFlags);
			playerFlags = FLAG_LUCK.setValue(client.player.hasStatusEffect(StatusEffects.LUCK), playerFlags);
			playerFlags = FLAG_UNLUCK.setValue(client.player.hasStatusEffect(StatusEffects.UNLUCK), playerFlags);
			playerFlags = FLAG_SLOW_FALLING.setValue(client.player.hasStatusEffect(StatusEffects.SLOW_FALLING), playerFlags);
			playerFlags = FLAG_CONDUIT_POWER.setValue(client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER), playerFlags);
			playerFlags = FLAG_DOLPHINS_GRACE.setValue(client.player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE), playerFlags);
			playerFlags = FLAG_BAD_OMEN.setValue(client.player.hasStatusEffect(StatusEffects.BAD_OMEN), playerFlags);
			playerFlags = FLAG_HERO_OF_THE_VILLAGE.setValue(client.player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE), playerFlags);

			if (world.getRegistryKey() == World.OVERWORLD) {
				worldFlags = FLAG_IS_OVERWORLD.setValue(true, worldFlags);
			} else if (world.getRegistryKey() == World.NETHER) {
				worldFlags = FLAG_IS_NETHER.setValue(true, worldFlags);
			} else if (world.getRegistryKey() == World.END) {
				worldFlags = FLAG_IS_END.setValue(true, worldFlags);
			}

			worldFlags = FLAG_IS_RAINING.setValue(world.isRaining(), worldFlags);
			worldFlags = FLAG_IS_SKY_DARKENED.setValue(world.getSkyProperties().isDarkened(), worldFlags);
			worldFlags = FLAG_IS_THUNDERING.setValue(world.isThundering(), worldFlags);

			DATA.put(RAIN_STRENGTH, world.getRainGradient(tickDelta));

			ItemLight light = ItemLight.NONE;

			if (player != null) {
				light = ItemLight.get(player.getMainHandStack());

				if (light == ItemLight.NONE) {
					light = ItemLight.get(player.getOffHandStack());
				}

				if (!light.worksInFluid() && player.isInsideWaterOrBubbleColumn()) {
					light = ItemLight.NONE;
				}
			}

			DATA.put(HELD_LIGHT_RED, light.red());
			DATA.put(HELD_LIGHT_GREEN, light.green());
			DATA.put(HELD_LIGHT_BLUE, light.blue());
			DATA.put(HELD_LIGHT_INTENSITY, light.intensity());

			DATA.put(AMBIENT_INTENSITY, world.method_23783(1.0F));
			DATA.put(MOON_SIZE, world.getMoonSize());

			final float fluidModifier = client.player.getUnderwaterVisibility();

			if (nightVision) {
				DATA.put(WORLD_EFFECT_MODIFIER, GameRenderer.getNightVisionStrength(client.player, tickDelta));
			} else if (fluidModifier > 0.0F && client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
				DATA.put(WORLD_EFFECT_MODIFIER, fluidModifier);
			} else {
				DATA.put(WORLD_EFFECT_MODIFIER, 0.0F);
			}
		}

		putViewVector(CAMERA_VIEW, camera.getYaw(), camera.getPitch());
		putViewVector(ENTITY_VIEW, cameraEntity.yaw, cameraEntity.pitch);

		FlagData.DATA.put(FlagData.WORLD_DATA_INDEX, worldFlags);
		FlagData.DATA.put(FlagData.PLAYER_DATA_INDEX, playerFlags);
	}

	public static void updateEmissiveColor(int color) {
		DATA.put(EMISSIVE_COLOR_RED, ((color >> 24) & 0xFF) / 255f);
		DATA.put(EMISSIVE_COLOR_GREEN, ((color >> 16) & 0xFF) / 255f);
		DATA.put(EMISSIVE_COLOR_BLUE, (color & 0xFF) / 255f);
	}

	private static void putViewVector(int index, float yaw, float pitch) {
		final float y = (float) Math.toRadians(yaw);
		final float p = (float) Math.toRadians(pitch);

		DATA.put(index, -MathHelper.sin(y) * MathHelper.cos(p));
		DATA.put(index + 1, -MathHelper.sin(p));
		DATA.put(index + 2, MathHelper.cos(y) * MathHelper.cos(p));
	}
}
