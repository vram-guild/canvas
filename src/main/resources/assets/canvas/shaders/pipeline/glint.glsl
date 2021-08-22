
#include frex:shaders/api/sampler.glsl
#include frex:shaders/api/fragment.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/pipeline/glint.glsl
******************************************************/

uniform sampler2D cvu_glint;

void glintify(inout vec4 a, float glint) {
	if (glint == 1.0) {
		float t = frx_renderSeconds * 0.25;
		vec2 tt = vec2(-t * 0.25, t);
		vec2 uv = frx_normalizeMappedUV(frx_texcoord) * 0.125;

		a += texture(cvu_glint, uv + tt);
		a = clamp(a, 0.0, 1.0);
	}
}
