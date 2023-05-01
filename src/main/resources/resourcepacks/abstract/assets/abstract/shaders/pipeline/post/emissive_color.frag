#include frex:shaders/api/header.glsl
#include abstract:shaders/pipeline/pipeline.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/lib/sample.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  abstract:shaders/pipeline/post/emissive_color.frag
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_emissive;

in vec2 _cvv_texcoord;
out vec4 fragColor;

void main() {
	vec4 e = texture(_cvu_emissive, _cvv_texcoord);
	vec4 c = frx_fromGamma(texture(_cvu_base, _cvv_texcoord));
	fragColor = vec4(c.rgb * e.rrr, 1.0);
}
