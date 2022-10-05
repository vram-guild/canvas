#include frex:shaders/api/header.glsl
#include abstract:shaders/pipeline/pipeline.glsl

/******************************************************
  abstract:shaders/pipeline/post/simple_full_frame.vert
******************************************************/

in vec3 in_vertex;
in vec2 in_uv;

out vec2 _cvv_texcoord;

void main() {
	vec4 outPos = frxu_frameProjectionMatrix * vec4(in_vertex.xy * frxu_size, 0.0, 1.0);
	gl_Position = vec4(outPos.xy, 0.2, 1.0);
	_cvv_texcoord = in_uv;
}
