/******************************************************
  canvas:shaders/pipeline/pipeline.glsl
******************************************************/
#define BLOOM_INTENSITY 0.1
#define BLOOM_DOWNSAMPLE_DIST 1.0
#define BLOOM_UPSAMPLE_DIST 0.1
#define BLOOM_CUTOFF 0.02

const vec2 BLOOM_DOWNSAMPLE_DIST_VEC = vec2 (BLOOM_DOWNSAMPLE_DIST, BLOOM_DOWNSAMPLE_DIST);
const vec2 BLOOM_UPSAMPLE_DIST_VEC = vec2 (BLOOM_UPSAMPLE_DIST, BLOOM_UPSAMPLE_DIST);

uniform ivec2 frxu_size;
uniform int frxu_lod;
