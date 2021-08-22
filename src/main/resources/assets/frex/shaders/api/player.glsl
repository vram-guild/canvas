#include canvas:shaders/internal/world.glsl

/****************************************************************
 * frex:shaders/api/player.glsl - Canvas Implementation
 ***************************************************************/

#define frx_effectModifier _cvu_world[_CV_LAST_CAMERA_POS].w
#define frx_heldLight _cvu_world[_CV_HELD_LIGHT_RGBI]
#define frx_heldLightInnerRadius _cvu_world[_CV_RENDER_INFO].z
#define frx_heldLightOuterRadius _cvu_world[_CV_RENDER_INFO].w

#define _CV_PLAYER_EFFECT(flagId) int((_cvu_flags[_CV_PLAYER_FLAGS_INDEX] >> flagId) & 1u)
#define frx_effectSpeed _CV_PLAYER_EFFECT(0)
#define frx_effectSlowness _CV_PLAYER_EFFECT(1)
#define frx_effectHast _CV_PLAYER_EFFECT(2)
#define frx_effectMiningFatigue _CV_PLAYER_EFFECT(3)
#define frx_effectStrength _CV_PLAYER_EFFECT(4)
#define frx_effectInstantHealth _CV_PLAYER_EFFECT(5)
#define frx_effectInstantDamage _CV_PLAYER_EFFECT(6)
#define frx_effectJumpBoost _CV_PLAYER_EFFECT(7)
#define frx_effectNausea _CV_PLAYER_EFFECT(8)
#define frx_effectRegeneration _CV_PLAYER_EFFECT(9)
#define frx_effectResistance _CV_PLAYER_EFFECT(10)
#define frx_effectFireResistance _CV_PLAYER_EFFECT(11)
#define frx_effectWaterBreathing _CV_PLAYER_EFFECT(12)
#define frx_effectInvisibility _CV_PLAYER_EFFECT(13)
#define frx_effectBlindness _CV_PLAYER_EFFECT(14)
#define frx_effectNightVision _CV_PLAYER_EFFECT(15)
#define frx_effectHunger _CV_PLAYER_EFFECT(16)
#define frx_effectWeakness _CV_PLAYER_EFFECT(17)
#define frx_effectPoison _CV_PLAYER_EFFECT(18)
#define frx_effectWither _CV_PLAYER_EFFECT(19)
#define frx_effectHealthBoost _CV_PLAYER_EFFECT(20)
#define frx_effectAbsorption _CV_PLAYER_EFFECT(21)
#define frx_effectSaturation _CV_PLAYER_EFFECT(22)
#define frx_effectGlowing _CV_PLAYER_EFFECT(23)
#define frx_effectLevitation _CV_PLAYER_EFFECT(24)
#define frx_effectLuck _CV_PLAYER_EFFECT(25)
#define frx_effectUnluck _CV_PLAYER_EFFECT(26)
#define frx_effectSlowFalling _CV_PLAYER_EFFECT(27)
#define frx_effectConduitPower _CV_PLAYER_EFFECT(28)
#define frx_effectDolphinsGrace _CV_PLAYER_EFFECT(29)
#define frx_effectBadOmen _CV_PLAYER_EFFECT(30)
#define frx_effectHeroOfTheVillage _CV_PLAYER_EFFECT(31)

#define _CV_PLAYER_FLAG(flagId) int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> flagId) & 1u)
#define frx_playerEyeInFluid _CV_PLAYER_FLAG(7)
#define frx_playerEyeInWater _CV_PLAYER_FLAG(8)
#define frx_playerEyeInLava _CV_PLAYER_FLAG(9)
#define frx_playerSneaking _CV_PLAYER_FLAG(10)
#define frx_playerSwimming _CV_PLAYER_FLAG(11)
#define frx_playerSneakingPose _CV_PLAYER_FLAG(12)
#define frx_playerSwimmingPose _CV_PLAYER_FLAG(13)
#define frx_playerCreative _CV_PLAYER_FLAG(14)
#define frx_playerSpectator _CV_PLAYER_FLAG(15)
#define frx_playerRiding _CV_PLAYER_FLAG(16)
#define frx_playerOnFire _CV_PLAYER_FLAG(17)
#define frx_playerSleeping _CV_PLAYER_FLAG(18)
#define frx_playerSprinting _CV_PLAYER_FLAG(19)
#define frx_playerWet _CV_PLAYER_FLAG(20)

#define frx_playerMood _cvu_world[_CV_CAMERA_POS].w
#define frx_eyePos _cvu_world[_CV_EYE_POSITION].xyz
#define frx_eyeBrightness _cvu_world[_CV_EYE_BRIGHTNESS].xy
#define frx_smoothedEyeBrightness _cvu_world[_CV_EYE_BRIGHTNESS].zw

#define FRX_EFFECT_SPEED 0 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_SLOWNESS 1 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_HASTE 2 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_MINING_FATIGUE 3 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_STRENGTH 4 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_INSTANT_HEALTH 5 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_INSTANT_DAMAGE 6 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_JUMP_BOOST 7 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_NAUSEA 8 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_REGENERATION 9 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_RESISTANCE 10 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_FIRE_RESISTANCE 11 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_WATER_BREATHING 12 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_INVISIBILITY 13 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_BLINDNESS 14 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_NIGHT_VISION 15 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_HUNGER 16 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_WEAKNESS 17 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_POISON 18 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_WITHER 19 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_HEALTH_BOOST 20 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_ABSORPTION 21 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_SATURATION 22 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_GLOWING 23 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_LEVITATION 24 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_LUCK 25 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_UNLUCK 26 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_SLOW_FALLING 27 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_CONDUIT_POWER 28 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_DOLPHINS_GRACE 29 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_BAD_OMEN 30 // DEPRECATED - DO NOT USE
#define FRX_EFFECT_HERO_OF_THE_VILLAGE 31 // DEPRECATED - DO NOT USE

#define frx_playerHasEffect(effect) (frx_bitValue(_cvu_flags[_CV_PLAYER_FLAGS_INDEX], effect) == 1)  // DEPRECATED - DO NOT USE

#define FRX_PLAYER_EYE_IN_FLUID 7 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_EYE_IN_WATER 8 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_EYE_IN_LAVA 9 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_SNEAKING 10 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_SWIMMING 11 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_SNEAKING_POSE 12 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_SWIMMING_POSE 13 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_CREATIVE 14 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_SPECTATOR 15 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_RIDING 16 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_ON_FIRE 17 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_SLEEPING 18 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_SPRINTING 19 // DEPRECATED - DO NOT USE
#define FRX_PLAYER_WET 20 // DEPRECATED - DO NOT USE

#define frx_playerFlag(flag) (frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], flag) == 1) // DEPRECATED - DO NOT USE
