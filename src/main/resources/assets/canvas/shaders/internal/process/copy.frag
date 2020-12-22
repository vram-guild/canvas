#include canvas:shaders/internal/process/header.glsl
#include canvas:shaders/pipeline/pipeline.glsl

/******************************************************
  canvas:shaders/internal/process/copy.frag
******************************************************/
uniform sampler2D _cvu_input;

varying vec2 _cvv_texcoord;

void main() {
	gl_FragData[0] = texture2D(_cvu_input, _cvv_texcoord);
}
