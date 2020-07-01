#include canvas:shaders/api/fragment.glsl
#include canvas:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/warm_glow.frag
******************************************************/

void cv_startFragment(inout cv_FragmentData fragData) {
	float e = cv_luminance(fragData.spriteColor.rgb);
	bool lit = e >  0.8 || (fragData.spriteColor.r - fragData.spriteColor.b) > 0.3f;
	fragData.emissivity = lit ? e : 0.0;
	fragData.diffuse = !lit;
	fragData.ao = !lit;
}
