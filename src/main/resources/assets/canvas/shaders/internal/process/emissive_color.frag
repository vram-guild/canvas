#include canvas:shaders/internal/process/header.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/lib/sample.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/internal/process/emissive_color.frag
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_extras;
uniform ivec2 _cvu_size;

varying vec2 _cvv_texcoord;

void main() {
	vec4 e = texture2D(_cvu_extras, _cvv_texcoord);
	vec4 c = frx_fromGamma(texture2D(_cvu_base, _cvv_texcoord));
	gl_FragData[0] = vec4(c.rgb * e.rrr, e.r);
}
