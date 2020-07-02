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

vec3 ColorFetch(vec2 coord) {
	vec4 c = texture2D(_cvu_input, coord);
 	return c.rgb;
}

float weights[5];
float offsets[5];

void main() {
    weights[0] = 0.19638062;
    weights[1] = 0.29675293;
    weights[2] = 0.09442139;
    weights[3] = 0.01037598;
    weights[4] = 0.00025940;

    offsets[0] = 0.00000000;
    offsets[1] = 1.41176471;
    offsets[2] = 3.29411765;
    offsets[3] = 5.17647059;
    offsets[4] = 7.05882353;

    vec2 uv = _cvv_texcoord;

    vec3 color = vec3(0.0);
    float weightSum = 0.0;

    if (uv.x < 0.52) {
        color += ColorFetch(uv) * weights[0];
        weightSum += weights[0];

        for(int i = 1; i < 5; i++) {
            vec2 offset = vec2(offsets[i]) / _cvu_size;
            color += ColorFetch(uv + offset * _cvu_distance) * weights[i];
            color += ColorFetch(uv - offset * _cvu_distance) * weights[i];
            weightSum += weights[i] * 2.0;
        }

        color /= weightSum;
    }

    gl_FragData[0] = vec4(color, 1.0);
}
