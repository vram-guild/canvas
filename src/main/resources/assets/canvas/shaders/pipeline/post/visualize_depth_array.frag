#include frex:shaders/api/header.glsl
#include canvas:shaders/pipeline/pipeline.glsl

/******************************************************
  canvas:shaders/pipeline/post/visualize_depth_array.frag
******************************************************/
uniform sampler2DArray _cvu_input;

varying vec2 _cvv_texcoord;

// rough approximation - will be more linear and visible but cannot be used for anything else
void main() {
	float depth = texture2DArray(_cvu_input, vec3(_cvv_texcoord, frxu_layer)).r;
	gl_FragData[0] = vec4(depth, depth, depth, 1.0);
}
