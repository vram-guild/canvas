#include frex:shaders/api/header.glsl
#include canvas:shaders/internal/varying.glsl
#include canvas:shaders/internal/flags.glsl
#include frex:shaders/api/material.glsl
#include frex:shaders/api/fragment.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/material_main.frag
******************************************************/

void _cv_startFragment(inout frx_FragmentData data) {
	int cv_programId = _cv_fragmentProgramId();

#include canvas:startfragment
}

void main() {
#ifndef PROGRAM_BY_UNIFORM
	if (_cv_programDiscard()) {
		discard;
	}
#endif
	frx_FragmentData fragData = frx_createPipelineFragment();

	_cv_startFragment(fragData);

	// PERF: varyings better here?
	if (_cv_getFlag(_CV_FLAG_CUTOUT) == 1.0) {
		float t = _cv_getFlag(_CV_FLAG_TRANSLUCENT_CUTOUT) == 1.0 ? _CV_TRANSLUCENT_CUTOUT_THRESHOLD : 0.5;

		if (fragData.spriteColor.a * fragData.vertexColor.a < t) {
			discard;
		}
	}

	frx_writePipelineFragment(fragData);
}
