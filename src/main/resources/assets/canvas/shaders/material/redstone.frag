#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/redstone.frag
******************************************************/

void frx_startFragment(inout frx_FragmentData fragData) {
	float r = frx_smootherstep(0.1, 0.5, dot(fragData.spriteColor.rgb, vec3(1.0, 0.0, -1.0)));
	float l = frx_smootherstep(0.8, 1.0, frx_luminance(fragData.spriteColor.rgb));
	fragData.emissivity = max(r, l);
}
