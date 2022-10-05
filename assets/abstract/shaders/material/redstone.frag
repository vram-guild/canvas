#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  abstract:shaders/material/redstone.frag
******************************************************/

void frx_materialFragment() {
#ifndef DEPTH_PASS
	float r = frx_smootherstep(0.1, 0.5, dot(frx_sampleColor.rgb, vec3(1.0, 0.0, -1.0)));
	float l = frx_smootherstep(0.8, 1.0, frx_luminance(frx_sampleColor.rgb));
	frx_fragEmissive = max(r, l);
#endif
}
