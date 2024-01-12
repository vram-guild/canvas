#include frex:shaders/api/header.glsl

/******************************************************
  canvas:shaders/pipeline/internal/pbr.vert
******************************************************/

uniform mat4 frxu_frameProjectionMatrix;
uniform ivec2 frxu_size;

in vec3 in_vertex;
in vec2 in_uv;

out vec2 frx_texcoord;

void main() {
	vec4 outPos = frxu_frameProjectionMatrix * vec4(in_vertex.xy * frxu_size, 0.0, 1.0);
	gl_Position = vec4(outPos.xy, 0.2, 1.0);
	frx_texcoord = in_uv;
}
