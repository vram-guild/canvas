#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/luminance_glow.frag
******************************************************/

void frx_startFragment(inout frx_FragmentData fragData) {
	float e = frx_luminance(fragData.spriteColor.rgb);
	fragData.emissivity = e * e;
}
