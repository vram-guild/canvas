/******************************************************
  frex:shaders/lib/sample.glsl

  Common sampling functions.
******************************************************/

// Temporally stable box filter, as described by Jorge Jiminez, 2014
// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
vec4 cv_sample13(sampler2D tex, vec2 uv, vec2 distance, int lod) {
    vec4 a = texture2DLod(tex, uv + distance * vec2(-1.0, -1.0), lod);
    vec4 b = texture2DLod(tex, uv + distance * vec2( 0.0, -1.0), lod);
    vec4 c = texture2DLod(tex, uv + distance * vec2( 1.0, -1.0), lod);
    vec4 d = texture2DLod(tex, uv + distance * vec2(-0.5, -0.5), lod);
    vec4 e = texture2DLod(tex, uv + distance * vec2( 0.5, -0.5), lod);
    vec4 f = texture2DLod(tex, uv + distance * vec2(-1.0,  0.0), lod);
    vec4 g = texture2DLod(tex, uv, lod);
    vec4 h = texture2DLod(tex, uv + distance * vec2( 1.0,  0.0), lod);
    vec4 i = texture2DLod(tex, uv + distance * vec2(-0.5,  0.5), lod);
    vec4 j = texture2DLod(tex, uv + distance * vec2( 0.5,  0.5), lod);
    vec4 k = texture2DLod(tex, uv + distance * vec2(-1.0,  1.0), lod);
    vec4 l = texture2DLod(tex, uv + distance * vec2( 0.0,  1.0), lod);
    vec4 m = texture2DLod(tex, uv + distance * vec2( 1.0,  1.0), lod);

    vec2 div = (1.0 / 4.0) * vec2(0.5, 0.125);

    vec4 o = (d + e + i + j) * div.x;
    o += (a + b + g + f) * div.y;
    o += (b + c + h + g) * div.y;
    o += (f + g + l + k) * div.y;
    o += (g + h + m + l) * div.y;

    return o;
}

// non-LOD version of above
vec4 cv_sample13(sampler2D tex, vec2 uv, vec2 distance) {
    vec4 a = texture2D(tex, uv + distance * vec2(-1.0, -1.0));
    vec4 b = texture2D(tex, uv + distance * vec2( 0.0, -1.0));
    vec4 c = texture2D(tex, uv + distance * vec2( 1.0, -1.0));
    vec4 d = texture2D(tex, uv + distance * vec2(-0.5, -0.5));
    vec4 e = texture2D(tex, uv + distance * vec2( 0.5, -0.5));
    vec4 f = texture2D(tex, uv + distance * vec2(-1.0,  0.0));
    vec4 g = texture2D(tex, uv);
    vec4 h = texture2D(tex, uv + distance * vec2( 1.0,  0.0));
    vec4 i = texture2D(tex, uv + distance * vec2(-0.5,  0.5));
    vec4 j = texture2D(tex, uv + distance * vec2( 0.5,  0.5));
    vec4 k = texture2D(tex, uv + distance * vec2(-1.0,  1.0));
    vec4 l = texture2D(tex, uv + distance * vec2( 0.0,  1.0));
    vec4 m = texture2D(tex, uv + distance * vec2( 1.0,  1.0));

    vec2 div = (1.0 / 4.0) * vec2(0.5, 0.125);

    vec4 o = (d + e + i + j) * div.x;
    o += (a + b + g + f) * div.y;
    o += (b + c + h + g) * div.y;
    o += (f + g + l + k) * div.y;
    o += (g + h + m + l) * div.y;

    return o;
}

// Used for bloom upsample, as described by Jorge Jiminez, 2014
// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
vec4 cv_sampleTent(sampler2D tex, vec2 uv, vec2 distance, int lod) {
    vec4 d = distance.xyxy * vec4(1.0, 1.0, -1.0, 0.0);

    vec4 sum = texture2DLod(tex, uv - d.xy, lod)
		+ texture2DLod(tex, uv - d.wy, lod) * 2.0
		+ texture2DLod(tex, uv - d.zy, lod)
		+ texture2DLod(tex, uv + d.zw, lod) * 2.0
		+ texture2DLod(tex, uv, lod) * 4.0
		+ texture2DLod(tex, uv + d.xw, lod) * 2.0
		+ texture2DLod(tex, uv + d.zy, lod)
		+ texture2DLod(tex, uv + d.wy, lod) * 2.0
		+ texture2DLod(tex, uv + d.xy, lod);

    return sum * (1.0 / 16.0);
}

// non-LOD version of above
vec4 cv_sampleTent(sampler2D tex, vec2 uv, vec2 distance) {
    vec4 d = distance.xyxy * vec4(1.0, 1.0, -1.0, 0.0);

    vec4 sum = texture2D(tex, uv - d.xy)
		+ texture2D(tex, uv - d.wy) * 2.0
		+ texture2D(tex, uv - d.zy)
		+ texture2D(tex, uv + d.zw) * 2.0
		+ texture2D(tex, uv) * 4.0
		+ texture2D(tex, uv + d.xw) * 2.0
		+ texture2D(tex, uv + d.zy)
		+ texture2D(tex, uv + d.wy) * 2.0
		+ texture2D(tex, uv + d.xy);

    return sum * (1.0 / 16.0);
}
