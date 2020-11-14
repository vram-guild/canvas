#include frex:shaders/api/vertex.glsl
#include frex:shaders/lib/face.glsl

/******************************************************
  canvas:shaders/material/lava.vert
******************************************************/

void frx_startVertex(inout frx_VertexData data) {
	if (abs(data.normal.y) < 0.001) {
		frx_var0.xy = frx_faceUv(data.vertex.xyz, data.normal.xyz);
		frx_var0.w = 1.0;
	} else {
		frx_var0.xy = frx_faceUv(data.vertex.xyz, FACE_UP);
		frx_var0.zw = -normalize(data.normal.xz);
	}
}

void frx_endVertex(inout frx_VertexData data) {
	// NOOP
}
