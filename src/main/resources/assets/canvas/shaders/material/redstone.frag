#include canvas:shaders/api/fragment.glsl
#include canvas:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/redstone.frag
******************************************************/

void cv_startFragment(inout cv_FragmentData fragData) {
	bool lit = (fragData.spriteColor.r - fragData.spriteColor.g) > 0.15f || luminance(fragData.spriteColor.rgb) > 0.9;
	fragData.emissive = lit;
	fragData.diffuse = !lit;
}
