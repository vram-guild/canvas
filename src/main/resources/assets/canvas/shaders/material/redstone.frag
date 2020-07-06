#include canvas:shaders/api/fragment.glsl
#include canvas:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/redstone.frag
******************************************************/

void cv_startFragment(inout cv_FragmentData fragData) {
	float r = cv_smootherstep(0.1, 0.5, dot(fragData.spriteColor.rgb, vec3(1.0, 0.0, -1.0)));
	float l = cv_smootherstep(0.8, 1.0, cv_luminance(fragData.spriteColor.rgb));
	fragData.emissivity = max(r, l);
}
