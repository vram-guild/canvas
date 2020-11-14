#include frex:shaders/api/vertex.glsl
#include frex:shaders/lib/face.glsl

void frx_startVertex(inout frx_VertexData data) {
	// 2D noise coordinates are derived from world geometry using a Canvas library function
	frx_var0.xy = frx_faceUv(data.vertex.xyz, data.normal);
}
