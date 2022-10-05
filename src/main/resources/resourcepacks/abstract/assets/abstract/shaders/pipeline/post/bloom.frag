#include frex:shaders/api/header.glsl
#include abstract:shaders/pipeline/pipeline.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/lib/sample.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/api/world.glsl
#include abstract:shaders/pipeline/post/bloom_options.glsl

/******************************************************
  abstract:shaders/pipeline/post/bloom.frag
******************************************************/

uniform sampler2D _cvu_base;
uniform sampler2D _cvu_bloom;

in vec2 _cvv_texcoord;
out vec4 fragColor;

// Based on approach described by Jorge Jiminez, 2014
// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
void main() {
	vec4 base = frx_fromGamma(texture(_cvu_base, _cvv_texcoord));
	vec4 bloom = textureLod(_cvu_bloom, _cvv_texcoord, 0) * BLOOM_INTENSITY;

	// ramp down the very low end to avoid halo banding
	vec3 cutoff = min(bloom.rgb, vec3(BLOOM_CUTOFF));
	vec3 ramp = cutoff / BLOOM_CUTOFF;
	ramp = ramp * ramp * BLOOM_CUTOFF;
	vec3 color = base.rgb + bloom.rgb - cutoff + ramp;

	fragColor = clamp(frx_toGamma(vec4(color, 1.0)), 0.0, 1.0);
}
