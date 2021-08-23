#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/warm_glow.frag
******************************************************/

void frx_materialFragment() {
#ifndef DEPTH_PASS
	float e = frx_luminance(frx_sampleColor.rgb);
	bool lit = e >  0.8 || (frx_sampleColor.r - frx_sampleColor.b) > 0.3f;
	frx_fragEmissive = lit ? e : 0.0;
	frx_fragEnableDiffuse = frx_fragEnableDiffuse && !lit;
	frx_fragEnableAo = frx_fragEnableAo && !lit;
#endif
}
