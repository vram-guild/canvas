#include canvas:shaders/internal/flags.glsl
#include canvas:shaders/internal/program.glsl

/******************************************************
  frex:shaders/api/material.glsl

  Utilities for querying material properties.
******************************************************/

#define frx_matEmissive _CV_GET_FLAG(_CV_FLAG_EMISSIVE)
#define frx_matCutout (((_cvv_flags >> _CV_CUTOUT_SHIFT) & _CV_CUTOUT_MASK) == _CV_CUTOUT_NONE ? 0 : 1)
#define frx_matUnmipped _CV_GET_FLAG(_CV_FLAG_UNMIPPED)
#define frx_matDisableAo _CV_GET_FLAG(_CV_FLAG_DISABLE_AO)
#define frx_matDisableDiffuse _CV_GET_FLAG(_CV_FLAG_DISABLE_DIFFUSE)
#define frx_matHurt _CV_GET_FLAG(_CV_FLAG_HURT_OVERLAY)
#define frx_matFlash _CV_GET_FLAG(_CV_FLAG_FLASH_OVERLAY)
#define frx_matGlint _CV_GET_FLAG(_CV_FLAG_GLINT)
#define frx_matExposure 0.0
// TODO
#define frx_matReflectance 0.04
#define frx_matRoughness 0.0
