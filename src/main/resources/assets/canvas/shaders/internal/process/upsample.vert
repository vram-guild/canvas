#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/downsample.vert
******************************************************/

uniform ivec2 _cvu_size;
uniform int _cvu_lod;

attribute vec2 in_uv;

varying vec2 _cvv_texcoord;

void main() {
	vec4 outPos = gl_ProjectionMatrix * vec4(gl_Vertex.xy * _cvu_size, 0.0, 1.0);
	gl_Position = vec4(outPos.xy, 0.2, 1.0);
	_cvv_texcoord = in_uv;
}
