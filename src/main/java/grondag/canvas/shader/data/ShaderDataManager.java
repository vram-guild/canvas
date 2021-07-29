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

package grondag.canvas.shader.data;

import static grondag.canvas.shader.data.FloatData.AMBIENT_INTENSITY;
import static grondag.canvas.shader.data.FloatData.ATMOSPHERIC_COLOR;
import static grondag.canvas.shader.data.FloatData.EMISSIVE_COLOR_BLUE;
import static grondag.canvas.shader.data.FloatData.EMISSIVE_COLOR_GREEN;
import static grondag.canvas.shader.data.FloatData.EMISSIVE_COLOR_RED;
import static grondag.canvas.shader.data.FloatData.EYE_LIGHT_BLOCK;
import static grondag.canvas.shader.data.FloatData.EYE_LIGHT_SKY;
import static grondag.canvas.shader.data.FloatData.EYE_POSITION;
import static grondag.canvas.shader.data.FloatData.FLOAT_VECTOR_DATA;
import static grondag.canvas.shader.data.FloatData.FOG_COLOR;
import static grondag.canvas.shader.data.FloatData.FOG_END;
import static grondag.canvas.shader.data.FloatData.FOG_START;
import static grondag.canvas.shader.data.FloatData.HELD_LIGHT_BLUE;
import static grondag.canvas.shader.data.FloatData.HELD_LIGHT_GREEN;
import static grondag.canvas.shader.data.FloatData.HELD_LIGHT_INNER_ANGLE;
import static grondag.canvas.shader.data.FloatData.HELD_LIGHT_INTENSITY;
import static grondag.canvas.shader.data.FloatData.HELD_LIGHT_OUTER_ANGLE;
import static grondag.canvas.shader.data.FloatData.HELD_LIGHT_RED;
import static grondag.canvas.shader.data.FloatData.MOON_SIZE;
import static grondag.canvas.shader.data.FloatData.NIGHT_VISION_STRENGTH;
import static grondag.canvas.shader.data.FloatData.PLAYER_MOOD;
import static grondag.canvas.shader.data.FloatData.RAIN_STRENGTH;
import static grondag.canvas.shader.data.FloatData.RENDER_SECONDS;
import static grondag.canvas.shader.data.FloatData.SKYLIGHT_COLOR;
import static grondag.canvas.shader.data.FloatData.SKYLIGHT_ILLUMINANCE;
import static grondag.canvas.shader.data.FloatData.SKYLIGHT_TRANSITION_FACTOR;
import static grondag.canvas.shader.data.FloatData.SKYLIGHT_VECTOR;
import static grondag.canvas.shader.data.FloatData.SKY_ANGLE_RADIANS;
import static grondag.canvas.shader.data.FloatData.SMOOTHED_EYE_LIGHT_BLOCK;
import static grondag.canvas.shader.data.FloatData.SMOOTHED_EYE_LIGHT_SKY;
import static grondag.canvas.shader.data.FloatData.SMOOTHED_RAIN_STRENGTH;
import static grondag.canvas.shader.data.FloatData.THUNDER_STRENGTH;
import static grondag.canvas.shader.data.FloatData.VEC_CAMERA_POS;
import static grondag.canvas.shader.data.FloatData.VEC_CAMERA_VIEW;
import static grondag.canvas.shader.data.FloatData.VEC_ENTITY_VIEW;
import static grondag.canvas.shader.data.FloatData.VEC_LAST_CAMERA_POS;
import static grondag.canvas.shader.data.FloatData.VEC_VANILLA_CLEAR_COLOR;
import static grondag.canvas.shader.data.FloatData.VIEW_ASPECT;
import static grondag.canvas.shader.data.FloatData.VIEW_BRIGHTNESS;
import static grondag.canvas.shader.data.FloatData.VIEW_DISTANCE;
import static grondag.canvas.shader.data.FloatData.VIEW_HEIGHT;
import static grondag.canvas.shader.data.FloatData.VIEW_WIDTH;
import static grondag.canvas.shader.data.FloatData.WORLD_DAYS;
import static grondag.canvas.shader.data.FloatData.WORLD_TIME;
import static grondag.canvas.shader.data.IntData.FLAG_ABSORPTION;
import static grondag.canvas.shader.data.IntData.FLAG_BAD_OMEN;
import static grondag.canvas.shader.data.IntData.FLAG_BLINDNESS;
import static grondag.canvas.shader.data.IntData.FLAG_CAMERA_IN_FLUID;
import static grondag.canvas.shader.data.IntData.FLAG_CAMERA_IN_LAVA;
import static grondag.canvas.shader.data.IntData.FLAG_CAMERA_IN_WATER;
import static grondag.canvas.shader.data.IntData.FLAG_CONDUIT_POWER;
import static grondag.canvas.shader.data.IntData.FLAG_CREATIVE;
import static grondag.canvas.shader.data.IntData.FLAG_DOLPHINS_GRACE;
import static grondag.canvas.shader.data.IntData.FLAG_EYE_IN_FLUID;
import static grondag.canvas.shader.data.IntData.FLAG_EYE_IN_LAVA;
import static grondag.canvas.shader.data.IntData.FLAG_EYE_IN_WATER;
import static grondag.canvas.shader.data.IntData.FLAG_FIRE_RESISTANCE;
import static grondag.canvas.shader.data.IntData.FLAG_GLOWING;
import static grondag.canvas.shader.data.IntData.FLAG_HASTE;
import static grondag.canvas.shader.data.IntData.FLAG_HAS_SKYLIGHT;
import static grondag.canvas.shader.data.IntData.FLAG_HEALTH_BOOST;
import static grondag.canvas.shader.data.IntData.FLAG_HERO_OF_THE_VILLAGE;
import static grondag.canvas.shader.data.IntData.FLAG_HUNGER;
import static grondag.canvas.shader.data.IntData.FLAG_INSTANT_DAMAGE;
import static grondag.canvas.shader.data.IntData.FLAG_INSTANT_HEALTH;
import static grondag.canvas.shader.data.IntData.FLAG_INVISIBILITY;
import static grondag.canvas.shader.data.IntData.FLAG_IS_END;
import static grondag.canvas.shader.data.IntData.FLAG_IS_NETHER;
import static grondag.canvas.shader.data.IntData.FLAG_IS_OVERWORLD;
import static grondag.canvas.shader.data.IntData.FLAG_IS_RAINING;
import static grondag.canvas.shader.data.IntData.FLAG_IS_SKY_DARKENED;
import static grondag.canvas.shader.data.IntData.FLAG_IS_THUNDERING;
import static grondag.canvas.shader.data.IntData.FLAG_JUMP_BOOST;
import static grondag.canvas.shader.data.IntData.FLAG_LEVITATION;
import static grondag.canvas.shader.data.IntData.FLAG_LUCK;
import static grondag.canvas.shader.data.IntData.FLAG_MINING_FATIGUE;
import static grondag.canvas.shader.data.IntData.FLAG_MOONLIT;
import static grondag.canvas.shader.data.IntData.FLAG_NAUSEA;
import static grondag.canvas.shader.data.IntData.FLAG_NIGHT_VISION;
import static grondag.canvas.shader.data.IntData.FLAG_ON_FIRE;
import static grondag.canvas.shader.data.IntData.FLAG_POISON;
import static grondag.canvas.shader.data.IntData.FLAG_REGENERATION;
import static grondag.canvas.shader.data.IntData.FLAG_RESISTANCE;
import static grondag.canvas.shader.data.IntData.FLAG_RIDING;
import static grondag.canvas.shader.data.IntData.FLAG_SATURATION;
import static grondag.canvas.shader.data.IntData.FLAG_SLEEPING;
import static grondag.canvas.shader.data.IntData.FLAG_SLOWNESS;
import static grondag.canvas.shader.data.IntData.FLAG_SLOW_FALLING;
import static grondag.canvas.shader.data.IntData.FLAG_SNEAKING;
import static grondag.canvas.shader.data.IntData.FLAG_SNEAKING_POSE;
import static grondag.canvas.shader.data.IntData.FLAG_SPECTATOR;
import static grondag.canvas.shader.data.IntData.FLAG_SPEED;
import static grondag.canvas.shader.data.IntData.FLAG_SPRINTING;
import static grondag.canvas.shader.data.IntData.FLAG_STRENGTH;
import static grondag.canvas.shader.data.IntData.FLAG_SWIMMING;
import static grondag.canvas.shader.data.IntData.FLAG_SWIMMING_POSE;
import static grondag.canvas.shader.data.IntData.FLAG_UNLUCK;
import static grondag.canvas.shader.data.IntData.FLAG_WATER_BREATHING;
import static grondag.canvas.shader.data.IntData.FLAG_WEAKNESS;
import static grondag.canvas.shader.data.IntData.FLAG_WET;
import static grondag.canvas.shader.data.IntData.FLAG_WITHER;
import static grondag.canvas.shader.data.IntData.INT_DATA;
import static grondag.canvas.shader.data.IntData.PLAYER_DATA_INDEX;
import static grondag.canvas.shader.data.IntData.RENDER_FRAMES;
import static grondag.canvas.shader.data.IntData.UINT_DATA;
import static grondag.canvas.shader.data.IntData.WORLD_DATA_INDEX;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.light.LightingProvider;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.varia.CelestialObjectFunction;
import grondag.canvas.varia.CelestialObjectFunction.CelestialObjectInput;
import grondag.canvas.varia.CelestialObjectFunction.CelestialObjectOutput;
import grondag.frex.api.event.HeldItemLightListener;
import grondag.frex.api.light.ItemLight;

public class ShaderDataManager {
	private static int worldFlags;
	private static int playerFlags;
	static long baseRenderTime = System.currentTimeMillis();
	static int renderFrames = 0;
	static double smoothedEyeLightBlock = 0;
	static double smoothedEyeLightSky = 0;
	static double smoothedRainStrength = 0;

	/** Camera view vector in world space - normalized. */
	public static final Vec3f cameraVector = new Vec3f();

	/** Points towards the light - normalized. */
	public static final Vec3f skyLightVector = new Vec3f();

	public static float cameraX, cameraY, cameraZ = 0f;

	// keep extra precision for terrain
	public static double cameraXd, cameraYd, cameraZd = 0;
	private static float tickDelta;
	private static ClientWorld world;

	private static final CelestialObjectOutput skyOutput = new CelestialObjectOutput();

	private static final CelestialObjectInput skyInput = new CelestialObjectInput() {
		@Override
		public ClientWorld world() {
			return world;
		}

		@Override
		public float tickDelta() {
			return tickDelta;
		}

		@Override
		public double cameraX() {
			return cameraXd;
		}

		@Override
		public double cameraY() {
			return cameraYd;
		}

		@Override
		public double cameraZ() {
			return cameraZd;
		}
	};

	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: WorldDataManager static init");
		}
	}

	public static void captureClearColor(float r, float g, float b) {
		FLOAT_VECTOR_DATA.put(VEC_VANILLA_CLEAR_COLOR, r);
		FLOAT_VECTOR_DATA.put(VEC_VANILLA_CLEAR_COLOR + 1, g);
		FLOAT_VECTOR_DATA.put(VEC_VANILLA_CLEAR_COLOR + 2, b);
	}

	private static void computeEyeNumbers(ClientWorld world, ClientPlayerEntity player) {
		float sky = 15, block = 15;

		FLOAT_VECTOR_DATA.put(EYE_POSITION, (float) player.getX());
		FLOAT_VECTOR_DATA.put(EYE_POSITION + 1, (float) player.getY());
		FLOAT_VECTOR_DATA.put(EYE_POSITION + 2, (float) player.getZ());

		final int eyeX = MathHelper.floor(player.getX());
		final int eyeZ = MathHelper.floor(player.getZ());

		if (eyeX >= -30000000 && eyeZ >= -30000000 && eyeX < 30000000 && eyeZ < 30000000) {
			if (world.isChunkLoaded(eyeX >> 4, eyeZ >> 4)) {
				final BlockPos eyePos = new BlockPos(eyeX, MathHelper.floor(player.getEyeY()), eyeZ);
				final LightingProvider lighter = world.getLightingProvider();
				computeEyeFlags(world, player, eyePos);

				if (lighter != null) {
					block = lighter.get(LightType.BLOCK).getLightLevel(eyePos);
					sky = Math.max(0, lighter.get(LightType.SKY).getLightLevel(eyePos) - world.getAmbientDarkness());
				}
			} else {
				worldFlags = FLAG_EYE_IN_FLUID.setValue(false, worldFlags);
				worldFlags = FLAG_EYE_IN_WATER.setValue(false, worldFlags);
				worldFlags = FLAG_EYE_IN_LAVA.setValue(false, worldFlags);
			}
		}

		// normalize
		sky /= 15f;
		block /= 15f;

		// Simple exponential smoothing
		final double a = 1.0 - Math.pow(Math.E, -1.0 / Pipeline.config().brightnessSmoothingFrames);

		if (Pipeline.config().smoothBrightnessBidirectionaly) {
			smoothedEyeLightBlock = smoothedEyeLightBlock * (1f - a) + a * block;
			smoothedEyeLightSky = smoothedEyeLightSky * (1f - a) + a * sky;
		} else {
			smoothedEyeLightBlock = block > smoothedEyeLightBlock ? block : smoothedEyeLightBlock * (1f - a) + a * block;
			smoothedEyeLightSky = sky > smoothedEyeLightSky ? sky : smoothedEyeLightSky * (1f - a) + a * sky;
		}

		FLOAT_VECTOR_DATA.put(EYE_LIGHT_BLOCK, block);
		FLOAT_VECTOR_DATA.put(EYE_LIGHT_SKY, sky);
		FLOAT_VECTOR_DATA.put(SMOOTHED_EYE_LIGHT_BLOCK, (float) smoothedEyeLightBlock);
		FLOAT_VECTOR_DATA.put(SMOOTHED_EYE_LIGHT_SKY, (float) smoothedEyeLightSky);
	}

	private static void computeEyeFlags(ClientWorld world, ClientPlayerEntity player, BlockPos eyePos) {
		final FluidState fluidState = world.getFluidState(eyePos);

		if (!fluidState.isEmpty()) {
			final double fluidHeight = eyePos.getY() + fluidState.getHeight(world, eyePos);

			if (fluidHeight >= player.getEyeY()) {
				worldFlags = FLAG_EYE_IN_FLUID.setValue(true, worldFlags);

				if (fluidState.getFluid().isIn(FluidTags.WATER)) {
					worldFlags = FLAG_EYE_IN_WATER.setValue(true, worldFlags);
				} else if (fluidState.getFluid().isIn(FluidTags.LAVA)) {
					worldFlags = FLAG_EYE_IN_LAVA.setValue(true, worldFlags);
				}
			}
		}
	}

	private static void computeCameraFlags(ClientWorld world, Camera camera) {
		final BlockPos cameraBlockPos = camera.getBlockPos();

		if (world.isInBuildLimit(cameraBlockPos) && world.isChunkLoaded(cameraBlockPos)) {
			final FluidState fluidState = world.getFluidState(cameraBlockPos);

			if (!fluidState.isEmpty()) {
				final double fluidHeight = cameraBlockPos.getY() + fluidState.getHeight(world, cameraBlockPos);

				if (fluidHeight >= camera.getPos().getY()) {
					worldFlags = FLAG_CAMERA_IN_FLUID.setValue(true, worldFlags);

					if (fluidState.getFluid().isIn(FluidTags.WATER)) {
						worldFlags = FLAG_CAMERA_IN_WATER.setValue(true, worldFlags);
					} else if (fluidState.getFluid().isIn(FluidTags.LAVA)) {
						worldFlags = FLAG_CAMERA_IN_LAVA.setValue(true, worldFlags);
					}
				}
			}
		} else {
			worldFlags = FLAG_CAMERA_IN_FLUID.setValue(false, worldFlags);
			worldFlags = FLAG_CAMERA_IN_WATER.setValue(false, worldFlags);
			worldFlags = FLAG_CAMERA_IN_LAVA.setValue(false, worldFlags);
		}
	}

	private static void updateRain(ClientWorld world, float tickDelta) {
		final float rain = world.getRainGradient(tickDelta);
		FLOAT_VECTOR_DATA.put(RAIN_STRENGTH, rain);
		FLOAT_VECTOR_DATA.put(THUNDER_STRENGTH, world.getThunderGradient(tickDelta));

		// Simple exponential smoothing
		final double a = 1.0 - Math.pow(Math.E, -1.0 / Pipeline.config().rainSmoothingFrames);
		smoothedRainStrength = smoothedRainStrength * (1f - a) + a * rain;
		FLOAT_VECTOR_DATA.put(SMOOTHED_RAIN_STRENGTH, (float) smoothedRainStrength);
	}

	// FEAT: make configurable by dimension, add smoothing
	/**
	 * Creates smooth transition between sun / moon direct lighting.
	 * @param tickTime 0 - 23999
	 * @return true if sky light is moon
	 */
	private static boolean computeSkylightFactor(long tickTime) {
		boolean result = false;
		float factor = 1;

		if (tickTime > 22000) {
			// sunrise
			if (tickTime < 23000) {
				// waning moon
				result = true;
				factor = (23000 - tickTime) / 1000f;
			} else if (tickTime < 24000) {
				// rising sun
				factor = (tickTime - 23000) / 1000f;
			}
		} else if (tickTime > 12000) {
			if (tickTime < 13000) {
				// waning sun
				factor = (13000 - tickTime) / 1000f;
			} else if (tickTime < 14000) {
				// rising moon
				factor = (tickTime - 13000) / 1000f;
				result = true;
			} else {
				result = true;
			}
		}

		FLOAT_VECTOR_DATA.put(SKYLIGHT_TRANSITION_FACTOR, factor);
		return result;
	}

	/**
	 * Called during render reload.
	 */
	public static void reload() {
		baseRenderTime = System.currentTimeMillis();
		renderFrames = 0;
	}

	/**
	 * Called just before terrain setup each frame after camera, fog and projection
	 * matrix are set up.
	 * @param projectionMatrix
	 * @param entry
	 */
	public static void update(Entry entry, Matrix4f projectionMatrix, Camera camera) {
		final MinecraftClient client = MinecraftClient.getInstance();
		final Entity cameraEntity = camera.getFocusedEntity();
		final float tickDelta = client.getTickDelta();
		assert cameraEntity != null;
		assert cameraEntity.getEntityWorld() != null;
		worldFlags = 0;
		playerFlags = 0;

		if (cameraEntity == null || cameraEntity.getEntityWorld() == null) {
			return;
		}

		FLOAT_VECTOR_DATA.put(RENDER_SECONDS, (System.currentTimeMillis() - baseRenderTime) / 1000f);
		FLOAT_VECTOR_DATA.put(VIEW_DISTANCE, client.options.viewDistance * 16);

		FLOAT_VECTOR_DATA.put(VEC_LAST_CAMERA_POS, cameraX);
		FLOAT_VECTOR_DATA.put(VEC_LAST_CAMERA_POS + 1, cameraY);
		FLOAT_VECTOR_DATA.put(VEC_LAST_CAMERA_POS + 2, cameraZ);
		final Vec3d cameraPos = camera.getPos();
		cameraXd = cameraPos.x;
		cameraX = (float) cameraXd;

		cameraYd = cameraPos.y;
		cameraY = (float) cameraYd;

		cameraZd = cameraPos.z;
		cameraZ = (float) cameraZd;

		FLOAT_VECTOR_DATA.put(VEC_CAMERA_POS, cameraX);
		FLOAT_VECTOR_DATA.put(VEC_CAMERA_POS + 1, cameraY);
		FLOAT_VECTOR_DATA.put(VEC_CAMERA_POS + 2, cameraZ);

		putViewVector(VEC_CAMERA_VIEW, camera.getYaw(), camera.getPitch(), cameraVector);
		putViewVector(VEC_ENTITY_VIEW, cameraEntity.getYaw(), cameraEntity.getPitch(), null);

		MatrixData.update(entry, projectionMatrix, camera, tickDelta);

		FLOAT_VECTOR_DATA.put(VIEW_WIDTH, PipelineManager.width());
		FLOAT_VECTOR_DATA.put(VIEW_HEIGHT, PipelineManager.height());
		FLOAT_VECTOR_DATA.put(VIEW_ASPECT, (float) PipelineManager.width() / (float) PipelineManager.height());
		FLOAT_VECTOR_DATA.put(VIEW_BRIGHTNESS, (float) client.options.gamma);

		final ClientWorld world = client.world;

		if (world != null) {
			final long days = world.getTimeOfDay() / 24000L;
			FLOAT_VECTOR_DATA.put(WORLD_DAYS, (int) (days % 2147483647L));
			final long tickTime = world.getTimeOfDay() - days * 24000L;
			final boolean skyLight = world.getDimension().hasSkyLight();
			FLOAT_VECTOR_DATA.put(WORLD_TIME, (float) (tickTime / 24000.0));
			final ClientPlayerEntity player = client.player;
			FLOAT_VECTOR_DATA.put(PLAYER_MOOD, player.getMoodPercentage());
			computeEyeNumbers(world, player);
			computeCameraFlags(world, camera);

			final float[] fogColor = RenderSystem.getShaderFogColor();
			FLOAT_VECTOR_DATA.put(FOG_COLOR, fogColor[0]);
			FLOAT_VECTOR_DATA.put(FOG_COLOR + 1, fogColor[1]);
			FLOAT_VECTOR_DATA.put(FOG_COLOR + 2, fogColor[2]);
			FLOAT_VECTOR_DATA.put(FOG_COLOR + 3, fogColor[3]);

			if (skyLight) {
				final boolean moonLight = computeSkylightFactor(tickTime);

				final float skyAngle = world.getSkyAngleRadians(tickDelta);

				// FEAT: fully implement celestial object model
				// should compute all objects and choose brightest as the skylight
				// and also apply dimension/pack settings
				ShaderDataManager.world = world;
				ShaderDataManager.tickDelta = tickDelta;

				skyOutput.zenithAngle = Pipeline.defaultZenithAngle;

				if (moonLight) {
					CelestialObjectFunction.VANILLA_MOON.compute(skyInput, skyOutput);
				} else {
					CelestialObjectFunction.VANILLA_SUN.compute(skyInput, skyOutput);
				}

				// Note this computes the value of skyLightVector
				ShadowMatrixData.update(camera, tickDelta, skyOutput);
				FLOAT_VECTOR_DATA.put(SKYLIGHT_VECTOR + 0, skyLightVector.getX());
				FLOAT_VECTOR_DATA.put(SKYLIGHT_VECTOR + 1, skyLightVector.getY());
				FLOAT_VECTOR_DATA.put(SKYLIGHT_VECTOR + 2, skyLightVector.getZ());
				FLOAT_VECTOR_DATA.put(SKY_ANGLE_RADIANS, skyAngle);

				worldFlags = FLAG_MOONLIT.setValue(moonLight, worldFlags);
				FLOAT_VECTOR_DATA.put(ATMOSPHERIC_COLOR + 0, skyOutput.atmosphericColorModifier.getX());
				FLOAT_VECTOR_DATA.put(ATMOSPHERIC_COLOR + 1, skyOutput.atmosphericColorModifier.getY());
				FLOAT_VECTOR_DATA.put(ATMOSPHERIC_COLOR + 2, skyOutput.atmosphericColorModifier.getZ());
				FLOAT_VECTOR_DATA.put(SKYLIGHT_COLOR + 0, skyOutput.lightColor.getX());
				FLOAT_VECTOR_DATA.put(SKYLIGHT_COLOR + 1, skyOutput.lightColor.getY());
				FLOAT_VECTOR_DATA.put(SKYLIGHT_COLOR + 2, skyOutput.lightColor.getZ());
				FLOAT_VECTOR_DATA.put(SKYLIGHT_ILLUMINANCE, skyOutput.illuminance);
			} else {
				FLOAT_VECTOR_DATA.put(SKYLIGHT_TRANSITION_FACTOR, 1);
				worldFlags = FLAG_MOONLIT.setValue(false, worldFlags);
				FLOAT_VECTOR_DATA.put(ATMOSPHERIC_COLOR + 0, 1);
				FLOAT_VECTOR_DATA.put(ATMOSPHERIC_COLOR + 1, 1);
				FLOAT_VECTOR_DATA.put(ATMOSPHERIC_COLOR + 2, 1);
				FLOAT_VECTOR_DATA.put(SKYLIGHT_COLOR + 0, 1);
				FLOAT_VECTOR_DATA.put(SKYLIGHT_COLOR + 1, 1);
				FLOAT_VECTOR_DATA.put(SKYLIGHT_COLOR + 2, 1);
				FLOAT_VECTOR_DATA.put(SKYLIGHT_ILLUMINANCE, 0);
			}

			worldFlags = FLAG_HAS_SKYLIGHT.setValue(skyLight, worldFlags);
			worldFlags = FLAG_SNEAKING.setValue(player.isInSneakingPose(), worldFlags);
			worldFlags = FLAG_SNEAKING_POSE.setValue(player.isSneaking(), worldFlags);
			worldFlags = FLAG_SWIMMING.setValue(player.isSwimming(), worldFlags);
			worldFlags = FLAG_SWIMMING_POSE.setValue(player.isInSwimmingPose(), worldFlags);
			worldFlags = FLAG_CREATIVE.setValue(player.isCreative(), worldFlags);
			worldFlags = FLAG_SPECTATOR.setValue(player.isSpectator(), worldFlags);
			worldFlags = FLAG_RIDING.setValue(player.isRiding(), worldFlags);
			worldFlags = FLAG_ON_FIRE.setValue(player.isOnFire(), worldFlags);
			worldFlags = FLAG_SLEEPING.setValue(player.isSleeping(), worldFlags);
			worldFlags = FLAG_SPRINTING.setValue(player.isSprinting(), worldFlags);
			worldFlags = FLAG_WET.setValue(player.isWet(), worldFlags);

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

			updateRain(world, tickDelta);

			ItemLight light = ItemLight.NONE;

			if (player != null) {
				ItemStack stack = player.getMainHandStack();
				light = ItemLight.get(stack);

				if (light == ItemLight.NONE) {
					stack = player.getOffHandStack();
					light = ItemLight.get(stack);
				}

				if (!light.worksInFluid() && player.isSubmergedInWater()) {
					light = ItemLight.NONE;
				}

				light = HeldItemLightListener.apply(player, stack, light);
			}

			FLOAT_VECTOR_DATA.put(HELD_LIGHT_RED, light.red());
			FLOAT_VECTOR_DATA.put(HELD_LIGHT_GREEN, light.green());
			FLOAT_VECTOR_DATA.put(HELD_LIGHT_BLUE, light.blue());
			FLOAT_VECTOR_DATA.put(HELD_LIGHT_INTENSITY, light.intensity());
			FLOAT_VECTOR_DATA.put(HELD_LIGHT_INNER_ANGLE, (float) (0.5 * Math.toRadians(light.innerConeAngleDegrees())));
			FLOAT_VECTOR_DATA.put(HELD_LIGHT_OUTER_ANGLE, (float) (0.5 * Math.toRadians(light.outerConeAngleDegrees())));

			FLOAT_VECTOR_DATA.put(AMBIENT_INTENSITY, world.method_23783(1.0F));
			FLOAT_VECTOR_DATA.put(MOON_SIZE, world.getMoonSize());

			final float fluidModifier = client.player.getUnderwaterVisibility();

			if (nightVision) {
				FLOAT_VECTOR_DATA.put(NIGHT_VISION_STRENGTH, GameRenderer.getNightVisionStrength(client.player, tickDelta));
			} else if (fluidModifier > 0.0F && client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
				FLOAT_VECTOR_DATA.put(NIGHT_VISION_STRENGTH, fluidModifier);
			}
		}

		INT_DATA.put(WORLD_DATA_INDEX, worldFlags);
		INT_DATA.put(PLAYER_DATA_INDEX, playerFlags);

		UINT_DATA.put(RENDER_FRAMES, renderFrames++);
	}

	/** Called when values are known to be good because vanilla resets them outside of world rendering. */
	public static void captureFogDistances() {
		FLOAT_VECTOR_DATA.put(FOG_START, RenderSystem.getShaderFogStart());
		FLOAT_VECTOR_DATA.put(FOG_END, RenderSystem.getShaderFogEnd());
	}

	public static void updateEmissiveColor(int color) {
		FLOAT_VECTOR_DATA.put(EMISSIVE_COLOR_RED, ((color >> 24) & 0xFF) / 255f);
		FLOAT_VECTOR_DATA.put(EMISSIVE_COLOR_GREEN, ((color >> 16) & 0xFF) / 255f);
		FLOAT_VECTOR_DATA.put(EMISSIVE_COLOR_BLUE, (color & 0xFF) / 255f);
	}

	private static void putViewVector(int index, float yaw, float pitch, Vec3f storeTo) {
		final Vec3d vec = Vec3d.fromPolar(pitch, yaw);
		final float x = (float) vec.x;
		final float y = (float) vec.y;
		final float z = (float) vec.z;

		FLOAT_VECTOR_DATA.put(index, x);
		FLOAT_VECTOR_DATA.put(index + 1, y);
		FLOAT_VECTOR_DATA.put(index + 2, z);

		if (storeTo != null) {
			storeTo.set(x, y, z);
		}
	}
}
