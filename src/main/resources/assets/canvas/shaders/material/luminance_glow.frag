#include canvas:shaders/api/fragment.glsl
#include canvas:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/luminance_glow.frag
******************************************************/

void cv_startFragment(inout cv_FragmentData fragData) {
	float e = cv_luminance(fragData.spriteColor.rgb);
	bool lit = e > 0.7;
	fragData.emissivity = lit ? 0.7 + 0.3 * e : 0.0;
	fragData.diffuse = !lit;
	fragData.ao = !lit;
}
