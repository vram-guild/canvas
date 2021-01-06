/******************************************************
  frex:shaders/api/context.glsl

  Definitions to indicate operating mode of renderer.
  Typical usage is to control conditional compilation of
  features that may not work or work differently in,
  for example, GUI vs world rendering.
******************************************************/

// If not present, lightmaps and other vanilla-specific data will not be valid or may not present.
// Access to vanilla lighting data should be guarded by #ifdef on this constant.
// Controlled by the active pipeline.
#define VANILLA_LIGHTING

// present in world context only when feature is enabled - if not present then foliage shaders should NOOP
#define ANIMATED_FOLIAGE


// Will define VERTEX_SHADER or FRAGMENT_SHADER - useful for checks in common libraries
// #define VERTEX_SHADER

// present only when pipeline support the feature and it is enabled
#define SHADOW_MAP_PRESENT
