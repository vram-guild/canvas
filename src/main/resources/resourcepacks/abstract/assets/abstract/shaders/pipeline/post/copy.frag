#include frex:shaders/api/header.glsl
#include abstract:shaders/pipeline/pipeline.glsl

/******************************************************
  abstract:shaders/pipeline/post/copy.frag
******************************************************/
uniform sampler2D _cvu_input;

in vec2 _cvv_texcoord;
out vec4 fragColor;

void main() {
	fragColor = texture(_cvu_input, _cvv_texcoord);
}
