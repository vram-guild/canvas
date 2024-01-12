#include frex:shaders/api/header.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  frex:shaders/func/normal_from_luminance.frag
******************************************************/

uniform sampler2D frxs_source0;
uniform ivec2 frxu_size;

in vec2 frx_texcoord;

out vec4 outColor;

void main() {
	float luminance = frx_luminance(texture(frxs_source0, frx_texcoord).rgb);
	outColor = vec4(normalize(vec3(dFdx(luminance), dFdy(luminance), luminance)) * 0.5 + 0.5, 1.0);
}
