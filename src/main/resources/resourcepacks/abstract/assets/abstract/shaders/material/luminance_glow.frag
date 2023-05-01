#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  abstract:shaders/material/luminance_glow.frag
******************************************************/

void frx_materialFragment() {
#ifndef DEPTH_PASS
	float e = frx_luminance(frx_sampleColor.rgb);
	frx_fragEmissive = e * e;
#endif
}
