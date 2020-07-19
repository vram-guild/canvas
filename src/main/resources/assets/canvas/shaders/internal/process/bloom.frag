#include canvas:shaders/internal/process/header.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/lib/sample.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/internal/process/bloom.frag
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_bloom;
uniform ivec2 _cvu_size;
uniform vec2 _cvu_distance;
uniform float cvu_intensity;

varying vec2 _cvv_texcoord;

// Based on approach described by Jorge Jiminez, 2014
// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
void main() {
    vec4 base = frx_fromGamma(texture2D(_cvu_base, _cvv_texcoord));

    vec4 bloom = texture2DLod(_cvu_bloom, _cvv_texcoord, 0);

    // chop off very low end to avoid halo banding
    vec3 color = base.rgb + (max(bloom.rgb - vec3(0.01), vec3(0))) / vec3(0.99) * cvu_intensity;

    gl_FragData[0] = clamp(frx_toGamma(vec4(color, 1.0)), 0.0, 1.0);
}
