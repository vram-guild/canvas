#include frex:shaders/api/header.glsl
#include canvas:shaders/internal/flags.glsl
#include frex:shaders/api/material.glsl
#include frex:shaders/api/fragment.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/material_main.frag
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
	frx_sampleColor = texture(frxs_baseColor, frx_texcoord, frx_matUnmippedFactor() * -4.0);

#ifdef _CV_FRAGMENT_COMPAT
	compatData = frx_FragmentData(!frx_matDisableDiffuse(), !frx_matDisableAo(), frx_sampleColor, frx_vertexColor);
#endif
	
	frx_fragColor = frx_sampleColor * frx_vertexColor;
	frx_fragReflectance = frx_matReflectance;
	frx_fragNormal = vec3(0.0, 0.0, 1.0);
	frx_fragHeight = 0;
	frx_fragRoughness = frx_matRoughness;
	frx_fragEmissive = frx_matEmissive;
	frx_fragLight = vec4(pv_lightcoord, 
			frx_matDisableDiffuse == 1 ? 1.0 : frx_vertexLight.z, 
			frx_matDisableAo == 1 ? 1.0 : frx_vertexLight.w);
	
	_cv_startFragment();

	if (frx_fragColor.a <= _cv_cutoutThreshold()) {
		discard;
	}

#ifdef _CV_FRAGMENT_COMPAT
	frx_fragLight.z = compatData.diffuse ? frx_fragLight.z : 1.0;
	frx_fragLight.w = compatData.ao ? frx_fragLight.w : 1.0;
#endif
	
	frx_pipelineFragment();
}
