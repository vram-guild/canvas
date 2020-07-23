#include frex:shaders/api/vertex.glsl
#include frex:shaders/lib/face.glsl

// sends noise coordinates from the vertex shader
varying vec2 v_noise_uv;

void frx_startVertex(inout frx_VertexData data) {
	// 2D noise coordinates are derived from world geometry using a Canvas library function
	v_noise_uv = frx_faceUv(data.vertex.xyz, data.normal);
}
