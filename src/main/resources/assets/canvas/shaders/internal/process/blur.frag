#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/blur.frag
******************************************************/
uniform sampler2D _cvu_input;
uniform ivec2 _cvu_size;
uniform vec2 _cvu_distance;

varying vec2 _cvv_texcoord;

// approach adapted from sonicether
// https://www.shadertoy.com/view/lstSRS

const float weights[5] = float[5](0.19638062, 0.29675293, 0.09442139, 0.01037598, 0.00025940);
const float offsets[5] = float[5](0.00000000, 1.41176471, 3.29411765, 5.17647059, 7.05882353);

void main() {
    vec4 color = texture2D(_cvu_input, _cvv_texcoord) * weights[0];
    vec2 f = _cvu_distance / _cvu_size;

	for(int i = 1; i < 5; i++) {
		vec2 offset = vec2(offsets[i]) * f;
		color += texture2D(_cvu_input, _cvv_texcoord + offset) * weights[i];
		color += texture2D(_cvu_input, _cvv_texcoord - offset) * weights[i];
	}

    gl_FragData[0] = vec4(color.rgb, 1.0);
}
