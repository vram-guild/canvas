#include frex:shaders/api/header.glsl
#include canvas:shaders/pipeline/pipeline.glsl
#include frex:shaders/api/view.glsl

/******************************************************
  canvas:shaders/pipeline/post/visualize_depth.frag
******************************************************/
uniform sampler2D _cvu_input;

in vec2 _cvv_texcoord;
out vec4 fragColor;

const float near = 0.05;
float far = frx_viewDistance() * 0.5f;

// rough approximation - will be more linear and visible but cannot be used for anything else
void main() {
	float depth = textureLod(_cvu_input, _cvv_texcoord, frxu_lod).r;
	float linearDepth = (2.0 * near) / (far + near - depth * (far - near));
	fragColor = vec4(linearDepth, linearDepth, linearDepth, 1.0);
}
