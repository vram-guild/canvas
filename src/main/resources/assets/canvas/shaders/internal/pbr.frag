#include frex:shaders/api/header.glsl

/******************************************************
  canvas:shaders/pipeline/internal/pbr.frag
******************************************************/

uniform frx_source0;

in vec2 frx_texcoord;

out vec4 outColor;

void main() {
	outColor = texture(frx_source0, frx_texcoord);
}
