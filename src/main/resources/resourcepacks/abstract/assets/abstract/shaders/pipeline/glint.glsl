
#include frex:shaders/api/sampler.glsl
#include frex:shaders/api/fragment.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  abstract:shaders/pipeline/glint.glsl
******************************************************/

uniform sampler2D cvu_glint;

void glintify(inout vec4 a, float glint) {
	if (glint == 1.0) {
		// vanilla scale factor for entity, works in most scenario
		const float scale = 0.16;

		// vanilla rotation factor
		const float angle = PI * 10. / 180.;
		const vec3  axis = vec3(0.0, 0.0, 1.0);
		const float s = sin(angle);
		const float c = cos(angle);
		const float oc = 1.0 - c;
		const mat4 rotation = mat4(
		oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0.0,
		oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0.0,
		oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0.0,
		0.0,                                0.0,                                0.0,                                1.0);

		// vanilla translation factor
		float time = frx_renderSeconds * 8.;
		float tx = mod(time, 110.) / 110.;
		float ty = mod(time, 30.) / 30.;
		vec2 translation = vec2(-tx, ty);

		vec2 uv = (rotation * vec4(frx_normalizeMappedUV(frx_texcoord) * scale, 0.0, 1.0)).xy + translation;
		vec4 glint = vec4(texture(cvu_glint, uv).rgb, 0.0);

		// emulate GL_SRC_COLOR sfactor
		a = clamp(a + glint * glint, 0.0, 1.0);
	}
}
