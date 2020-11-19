#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/lib/noise/cellular2x2x2.glsl

/******************************************************
  canvas:shaders/material/water.frag
******************************************************/

void frx_startFragment(inout frx_FragmentData fragData) {
	float t = frx_renderSeconds();
	vec2 uv = frx_var0.xy * 2.00 + t * frx_var0.zw;
	float n = cellular2x2x2(vec3(uv.xy, t * 0.4)).x;
	n = n * n * 0.35 + 0.65;
	fragData.spriteColor = vec4(n, n, n, n);
}
