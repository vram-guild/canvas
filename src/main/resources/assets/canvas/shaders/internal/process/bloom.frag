#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/bloom.frag
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_bloom;
uniform ivec2 _cvu_size;

varying vec2 _cvv_texcoord;

// approach adapted from sonicether
// https://www.shadertoy.com/view/lstSRS
vec3 saturate(vec3 x) {
    return clamp(x, vec3(0.0), vec3(1.0));
}

vec4 cubic(float x) {
    float x2 = x * x;
    float x3 = x2 * x;
    vec4 w;
    w.x =  -x3 + 3.0 * x2 - 3.0 * x + 1.0;
    w.y =  3.0 * x3 - 6.0 * x2 + 4.0;
    w.z = -3.0 * x3 + 3.0 * x2 + 3.0 * x + 1.0;
    w.w = x3;
    return w / 6.0;
}

vec4 bloom() {
	vec2 coord = _cvv_texcoord * _cvu_size;

	float fx = fract(coord.x);
    float fy = fract(coord.y);
    coord.x -= fx;
    coord.y -= fy;

    fx -= 0.5;
    fy -= 0.5;

    vec4 xcubic = cubic(fx);
    vec4 ycubic = cubic(fy);

    vec4 c = vec4(coord.x - 0.5, coord.x + 1.5, coord.y - 0.5, coord.y + 1.5);
    vec4 s = vec4(xcubic.x + xcubic.y, xcubic.z + xcubic.w, ycubic.x + ycubic.y, ycubic.z + ycubic.w);
    vec4 offset = c + vec4(xcubic.y, xcubic.w, ycubic.y, ycubic.w) / s;

    vec2 p0 = vec2(offset.x, offset.z) / _cvu_size;
    vec2 p1 = vec2(offset.y, offset.z) / _cvu_size;
    vec2 p2 = vec2(offset.x, offset.w) / _cvu_size;
    vec2 p3 = vec2(offset.y, offset.w) / _cvu_size;

    vec4 sample0 = texture2DLod(_cvu_bloom, p0, 0.0);
    vec4 sample1 = texture2DLod(_cvu_bloom, p1, 0.0);
    vec4 sample2 = texture2DLod(_cvu_bloom, p2, 0.0);
    vec4 sample3 = texture2DLod(_cvu_bloom, p3, 0.0);

    sample0 += texture2DLod(_cvu_bloom, p0, 1.0) * 1.5;
    sample1 += texture2DLod(_cvu_bloom, p1, 1.0) * 1.5;
    sample2 += texture2DLod(_cvu_bloom, p2, 1.0) * 1.5;
    sample3 += texture2DLod(_cvu_bloom, p3, 1.0) * 1.5;

    sample0 += texture2DLod(_cvu_bloom, p0, 2.0);
    sample1 += texture2DLod(_cvu_bloom, p1, 2.0);
    sample2 += texture2DLod(_cvu_bloom, p2, 2.0);
    sample3 += texture2DLod(_cvu_bloom, p3, 2.0);

    sample0 += texture2DLod(_cvu_bloom, p0, 3.0) * 1.5;
    sample1 += texture2DLod(_cvu_bloom, p1, 3.0) * 1.5;
    sample2 += texture2DLod(_cvu_bloom, p2, 3.0) * 1.5;
    sample3 += texture2DLod(_cvu_bloom, p3, 3.0) * 1.5;

    sample0 += texture2DLod(_cvu_bloom, p0, 4.0) * 1.8;
    sample1 += texture2DLod(_cvu_bloom, p1, 4.0) * 1.8;
    sample2 += texture2DLod(_cvu_bloom, p2, 4.0) * 1.8;
    sample3 += texture2DLod(_cvu_bloom, p3, 4.0) * 1.8;

    sample0 += texture2DLod(_cvu_bloom, p0, 5.0);
	sample1 += texture2DLod(_cvu_bloom, p1, 5.0);
	sample2 += texture2DLod(_cvu_bloom, p2, 5.0);
	sample3 += texture2DLod(_cvu_bloom, p3, 5.0);

    sample0 += texture2DLod(_cvu_bloom, p0, 6.0);
	sample1 += texture2DLod(_cvu_bloom, p1, 6.0);
	sample2 += texture2DLod(_cvu_bloom, p2, 6.0);
	sample3 += texture2DLod(_cvu_bloom, p3, 6.0);

    sample0 += texture2DLod(_cvu_bloom, p0, 7.0);
	sample1 += texture2DLod(_cvu_bloom, p1, 7.0);
	sample2 += texture2DLod(_cvu_bloom, p2, 7.0);
	sample3 += texture2DLod(_cvu_bloom, p3, 7.0);

    float sx = s.x / (s.x + s.y);
    float sy = s.z / (s.z + s.w);

    return mix( mix(sample3, sample2, sx), mix(sample1, sample0, sx), sy);
}

vec4 fastBloom() {
    return texture2DLod(_cvu_bloom, _cvv_texcoord, 0.0)
    	+ texture2DLod(_cvu_bloom, _cvv_texcoord, 1.0) * 1.5
    	+ texture2DLod(_cvu_bloom, _cvv_texcoord, 2.0)
    	+ texture2DLod(_cvu_bloom, _cvv_texcoord, 3.0) * 1.5
    	+ texture2DLod(_cvu_bloom, _cvv_texcoord, 4.0) * 1.8
    	+ texture2DLod(_cvu_bloom, _cvv_texcoord, 5.0)
    	+ texture2DLod(_cvu_bloom, _cvv_texcoord, 6.0)
    	+ texture2DLod(_cvu_bloom, _cvv_texcoord, 7.0);
}

void main() {
    vec4 color = texture2D(_cvu_base, _cvv_texcoord);

    color += clamp(bloom() * 0.12, 0.0, 1.0);

//    color += clamp(fastBloom() * 0.12, 0.0, 1.0);

    gl_FragData[0] = vec4(color.rgb, 1.0);
}
