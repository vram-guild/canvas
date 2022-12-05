#include frex:shaders/api/header.glsl
#include canvas:shaders/pipeline/pipeline.glsl
#include frex:shaders/api/view.glsl

/******************************************************
  canvas:shaders/pipeline/post/visualize_cube_map.frag
******************************************************/
uniform samplerCube _cvu_input;

in vec2 _cvv_texcoord;
out vec4 fragColor;

void main() {
    vec2 ndcXY = gl_FragCoord.xy / vec2(frxu_size.xy) * 2.0 - 1.0;
    vec3 ndcNear = vec3(ndcXY, -1.0);
    vec3 ndcFar  = vec3(ndcXY,  1.0);

    vec4 near0 = frx_inverseViewProjectionMatrix * vec4(ndcNear, 1.0);
    vec3 near = near0.xyz / near0.w;

    vec4 far0  = frx_inverseViewProjectionMatrix * vec4(ndcFar,  1.0);
    vec3 far  = far0.xyz / far0.w;

	fragColor = textureLod(_cvu_input, far - near, frxu_lod);
}
