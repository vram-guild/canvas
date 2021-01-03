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

/** Multiplicative version frx_matEmissive(), true return 1, false returns 0 */
float frx_matEmissiveFactor() {
	return _cv_getFlag(_CV_FLAG_EMISSIVE);
}

/*
 * True when material is cutout. When enabled,
 * fragments will be discarded if alpha < 0.5.
 */
bool frx_matCutout() {
	return _cv_getFlag(_CV_FLAG_CUTOUT) == 1.0;
}

/** Multiplicative version frx_matCutout(), true return 1, false returns 0 */
float frx_matCutoutFactor() {
	return _cv_getFlag(_CV_FLAG_CUTOUT);
}

/*
 * True when material is has Level of Detail (mip mapping) disabled.
 * Currently the RenderMaterail finder only allows this for cutout materials.
 */
bool frx_matUnmipped() {
	return _cv_getFlag(_CV_FLAG_UNMIPPED) == 1.0;
}

/** Multiplicative version frx_matUnmipped(), true return 1, false returns 0 */
float frx_matUnmippedFactor() {
	return _cv_getFlag(_CV_FLAG_UNMIPPED);
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

/**
 * RESERVED FOR FUTURE FEATURE - not yet implemented.
 *
 * Coarse indication of where the surface is located.
 * Zero means the surface is not exposed to the sky and
 * cannot get wet or otherwise be affected from directly above.
 *
 * Values 1.0, 2.0 and 3.0 indicate icy, temperate or hot biome
 * temperatures, respectively.
 *
 * Values > 0 also mean the surface exposed to the sky.
 * Does not mean it is facing up - check normals for that -
 * but does it mean it could get wet/icy/etc.
 */
float frx_matExposure() {
	return 0.0;
}
