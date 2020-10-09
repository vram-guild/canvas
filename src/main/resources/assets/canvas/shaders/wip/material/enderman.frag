#include frex:shaders/wip/api/fragment.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/enderman.frag
******************************************************/

void frx_startFragment(inout frx_FragmentData fragData) {
	if (frx_noise2d(_cvv_texcoord) < 0.5) {
		discard;
	}
}
