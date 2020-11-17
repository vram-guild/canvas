#include canvas:shaders/internal/process/header.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/lib/math.glsl

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
    vec4 color      = mix(base, max(base, reflection), gloss * 0.6);
    // vec4 metalColor1= mix(base, reflection, 0.5);
    // vec4 metalColor2= base - reflection + base * reflection;
    vec4 albedo     = base / max(base.r, max(base.g, base.b));
    vec4 metalColor = frx_toGamma(frx_fromGamma(base) + (albedo * albedo * frx_fromGamma(reflection)) * gloss);
    // vec4 metalColor = mix(base * 0.5, max(base * 0.5, albedo * albedo * reflection), gloss);
    gl_FragData[0]  = mix(color, metalColor, metal);
}