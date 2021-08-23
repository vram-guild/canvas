#include frex:shaders/api/header.glsl
#define DEPTH_PASS
#include canvas:shaders/internal/flags.glsl
#include frex:shaders/api/material.glsl
#include frex:shaders/api/fragment.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/shadow_main.frag
******************************************************/

void _cv_startFragment() {
	int cv_programId = _cv_fragmentProgramId();

#include canvas:startfragment
}

void main() {
#ifndef PROGRAM_BY_UNIFORM
	if (_cv_programDiscard()) {
		discard;
	}
#endif
	frx_sampleColor = texture(frxs_baseColor, frx_texcoord, frx_matUnmipped * -4.0);

#ifdef _CV_FRAGMENT_COMPAT
	compatData = frx_FragmentData(frx_sampleColor, frx_vertexColor);
#endif
	
	frx_fragColor = frx_sampleColor * frx_vertexColor;

	_cv_startFragment();

	if (frx_fragColor.a <= _cv_cutoutThreshold()) {
		discard;
	}

	frx_pipelineFragment();
}
