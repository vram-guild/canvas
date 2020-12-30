#include canvas:shaders/internal/flags.glsl

/******************************************************
  frex:shaders/api/material.glsl

  Utilities for querying material properties.
******************************************************/

/*
 * True when material is emissive.
 * For emissive materials, this is on/off, not a range.
 */
bool frx_matEmissive() {
	return _cv_getFlag(_CV_FLAG_EMISSIVE) == 1.0;
}

/*
 * True when material is cutout. When enabled,
 * fragments will be discarded if alpha < 0.5.
 */
bool frx_matCutout() {
	return _cv_getFlag(_CV_FLAG_CUTOUT) == 1.0;
}

/*
 * True when material is has Level of Detail (mip mapping) disabled.
 * Currently the RenderMaterail finder only allows this for cutout materials.
 */
bool frx_matUnmipped() {
	return _cv_getFlag(_CV_FLAG_UNMIPPED) == 1.0;
}

/*
 * True when material is marked to disable ambient occlusion shading.
 */
bool frx_matDisableAo() {
	return _cv_getFlag(_CV_FLAG_DISABLE_AO) == 1.0;
}

/**
 * True when material is marked to disable "diffuse" shading.
 * This may have a different or no effect in non-vanilla lighting models.
 */
bool frx_matDisableDiffuse() {
	return _cv_getFlag(_CV_FLAG_DISABLE_DIFFUSE) == 1.0;
}

/**
 * True when should render the red "hurt" overlay.
 * Mostly for use in pipeline shaders - material shaders aren't expected to handle.
 */
bool frx_matHurt() {
	return _cv_getFlag(_CV_FLAG_HURT_OVERLAY) == 1.0;
}

/**
 * True when should render the white "flash" overlay.
 * Mostly for use in pipeline shaders - material shaders aren't expected to handle.
 */
bool frx_matFlash() {
	return _cv_getFlag(_CV_FLAG_FLASH_OVERLAY) == 1.0;
}
