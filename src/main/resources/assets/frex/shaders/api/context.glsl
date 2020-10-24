/******************************************************
  frex:shaders/api/context.glsl

  Definitions to indicate operating mode of renderer.
  Typical usage is to control conditional compilation of
  features that may not work or work differently in,
  for example, GUI vs world rendering.
******************************************************/

// If not present, lightmaps and other vanilla-specific data may not be valid or present
#define VANILLA_LIGHTING

// present in world context only when feature is enabled - if not present then foliage shaders should NOOP
#define ANIMATED_FOLIAGE
