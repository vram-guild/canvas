#include frex:shaders/api/header.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  frex:shaders/func/luminance.frag
******************************************************/

uniform sampler2D frxs_source0;

in vec2 frx_texcoord;

out vec4 outColor;

void main() {
	float luminance = frx_luminance(texture(frxs_source0, frx_texcoord).rgb);
	outColor = vec4(luminance);
}
