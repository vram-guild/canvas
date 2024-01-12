#include frex:shaders/api/header.glsl

/******************************************************
  frex:shaders/func/green.frag
******************************************************/

uniform sampler2D frxs_source0;

in vec2 frx_texcoord;

out vec4 outColor;

void main() {
	outColor = vec4(texture(frxs_source0, frx_texcoord).g);
}
