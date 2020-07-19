#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/warm_glow.frag
******************************************************/

void frx_startFragment(inout frx_FragmentData fragData) {
	float e = frx_luminance(fragData.spriteColor.rgb);
	bool lit = e >  0.8 || (fragData.spriteColor.r - fragData.spriteColor.b) > 0.3f;
	fragData.emissivity = lit ? e : 0.0;
	fragData.diffuse = !lit;
	fragData.ao = !lit;
}
