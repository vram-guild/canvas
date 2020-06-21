#include canvas:shaders/api/fragment.glsl
#include canvas:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/redstone.frag
******************************************************/

void cv_startFragment(inout cv_FragmentData fragData) {
	bool lit = (fragData.spriteColor.r - fragData.spriteColor.g) > 0.15f || cv_luminance(fragData.spriteColor.rgb) > 0.9;
	fragData.emissivity = lit ? 1.0 : 0.0;
	fragData.diffuse = !lit;
	fragData.ao = !lit;
}
