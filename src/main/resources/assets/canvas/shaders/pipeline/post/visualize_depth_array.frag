#include frex:shaders/api/header.glsl
#include canvas:shaders/pipeline/pipeline.glsl

/******************************************************
  canvas:shaders/pipeline/post/visualize_depth_array.frag
******************************************************/
uniform sampler2DArray _cvu_input;

in vec2 _cvv_texcoord;
out vec4 fragColor;

// rough approximation - will be more linear and visible but cannot be used for anything else
void main() {
	float depth = textureLod(_cvu_input, vec3(_cvv_texcoord, frxu_layer), frxu_lod).r;
	fragColor = vec4(depth, depth, depth, 1.0);
}
