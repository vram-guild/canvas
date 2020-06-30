#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/blur.frag
******************************************************/
uniform sampler2D _cvu_input;
uniform ivec2 _cvu_size;
uniform vec2 _cvu_distance;

varying vec2 _cvv_texcoord;

void main() {
	vec2 uv = _cvv_texcoord;

	vec4 color = texture2D(_cvu_input, uv) * 0.19638062;

	vec2 offset1 = _cvu_distance * 1.41176471;
	color += texture2D(_cvu_input, uv + offset1) * 0.29675293;
	color += texture2D(_cvu_input, uv - offset1) * 0.29675293;

	vec2 offset2 = _cvu_distance * 3.29411765;
	color += texture2D(_cvu_input, uv + offset2) * 0.09442139;
	color += texture2D(_cvu_input, uv - offset2) * 0.09442139;

	vec2 offset3 = _cvu_distance * 5.17647059;
	color += texture2D(_cvu_input, uv + offset3) * 0.01037598;
	color += texture2D(_cvu_input, uv - offset3) * 0.01037598;

	vec2 offset4 = _cvu_distance * 7.05882353;
	color += texture2D(_cvu_input, uv + offset4) * 0.00025940;
	color += texture2D(_cvu_input, uv - offset4) * 0.00025940;

	gl_FragData[0] =  color;
}
