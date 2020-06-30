#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/bloom.frag
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_bloom0;
uniform sampler2D _cvu_bloom1;
uniform sampler2D _cvu_bloom2;
uniform ivec2 _cvu_size;

varying vec2 _cvv_texcoord;

#define F 0.2

void main() {
	vec4 base = texture2D(_cvu_base, _cvv_texcoord);
	vec4 b0 = texture2D(_cvu_bloom0, _cvv_texcoord) * F;
	vec4 b1 = texture2D(_cvu_bloom1, _cvv_texcoord) * F;
	vec4 b2 = texture2D(_cvu_bloom2, _cvv_texcoord) * F;                                                                                                                                                                                           ;

	vec3 c = clamp(base.rgb + b0.rgb + b1.rgb + b2.rgb, 0.0, 1.0);
	gl_FragData[0] = vec4(c, base.a);
}
