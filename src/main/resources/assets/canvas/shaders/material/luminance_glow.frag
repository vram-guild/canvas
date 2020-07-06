#include canvas:shaders/api/fragment.glsl
#include canvas:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/luminance_glow.frag
******************************************************/

void cv_startFragment(inout cv_FragmentData fragData) {
	float e = cv_luminance(fragData.spriteColor.rgb);
	fragData.emissivity = e * e;
}
