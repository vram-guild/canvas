#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/reflection.frag
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_reflection;
uniform sampler2D _cvu_extras;
uniform ivec2 _cvu_size;

varying vec2 _cvv_texcoord;

void main() {
    vec4 base 	    = texture2D(_cvu_base, _cvv_texcoord);
	vec4 reflection = texture2D(_cvu_reflection, _cvv_texcoord);
	float metal     = texture2DLod(_cvu_extras, _cvv_texcoord, 0).g;
	float gloss     = 1 - texture2DLod(_cvu_extras, _cvv_texcoord, 0).b;
    vec4 color      = base + base * vec4(vec3(1.0) - base.rgb, 1.0)  * reflection;
    vec4 metalColor = base * base + reflection;
    gl_FragData[0]  = mix(color, metalColor, metal);
}