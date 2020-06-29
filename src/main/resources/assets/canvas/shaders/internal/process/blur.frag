#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/copy.frag
******************************************************/
uniform sampler2D _cvu_input;
uniform ivec2 _cvu_size;
uniform vec2 _cvu_distance;

varying vec2 _cvv_texcoord;

void main() {
	gl_FragData[0] = 0.375 * texture2D(_cvu_input, _cvv_texcoord)
		+ 0.3125 * (texture2D(_cvu_input, _cvv_texcoord - _cvu_distance) + texture2D(_cvu_input, _cvv_texcoord + _cvu_distance));
}
