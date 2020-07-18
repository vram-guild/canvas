#include canvas:shaders/internal/flags.glsl

/******************************************************
  canvas:shaders/api/material.glsl

  Utilities for querying material properties.
******************************************************/

/*
 * True when material is emissive.
 * For emissive materials, this is on/off, not a range.
 */
bool cv_matEmissive() {
	return _cv_getFlag(_CV_FLAG_EMISSIVE) == 1.0;
}

/*
 * True when material is cutout. When enabled,
 * fragments will be discarded if alpha < 0.5.
 */
bool cv_matCutout() {
	return _cv_getFlag(_CV_FLAG_CUTOUT) == 1.0;
}

/*
 * True when material is has Level of Detail (mip mapping) disabled.
 * Currently the RenderMaterail finder only allows this for cutout materials.
 */
bool cv_matUnmipped() {
	return _cv_getFlag(_CV_FLAG_UNMIPPED) == 1.0;
}

/*
 * True when material is marked to disable ambient occlusion shading.
 */
bool cv_matDisableAo() {
	return _cv_getFlag(_CV_FLAG_DISABLE_AO) == 1.0;
}

/**
 * True when material is marked to disable "diffuse" shading.
 * This may have a different or no effect in non-vanilla lighting models.
 */
bool cv_matDisableDiffuse() {
	return _cv_getFlag(_CV_FLAG_DISABLE_DIFFUSE) == 1.0;
}
