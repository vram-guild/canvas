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

// Present only when pipeline supports the feature and it is enabled
#define SHADOW_MAP_PRESENT

// Present only when shadow map enabled
#define SHADOW_MAP_SIZE 1024

// Present when material shaders are being run to generate a shadow map or depth math
//#define DEPTH_PASS

// Present when extended texture maps to support Physically-Based Rendering are available
// Will not be defined during depth pass.
//#define PBR_ENABLED
