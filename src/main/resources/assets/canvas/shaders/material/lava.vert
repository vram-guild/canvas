#include frex:shaders/api/vertex.glsl
#include frex:shaders/lib/face.glsl

/******************************************************
  canvas:shaders/material/lava.vert
******************************************************/

void frx_startVertex(inout frx_VertexData data) {
	if (abs(data.normal.y) < 0.001) {
	    frx_var0.xy = frx_faceUv(data.vertex.xyz, data.normal.xyz);
	    //must specify frx_var0.z explicitly on Nvidia cards
	    frx_var0.zw = vec2(0.0, 1.0);
	} else {
	    frx_var0.xy = frx_faceUv(data.vertex.xyz, FACE_UP);
	    // apparently can't normalize (0, 0) on Nvidia cards
	    frx_var0.zw = abs(data.normal.y)==1.0 ? vec2(0.0, 0.0) : -normalize(data.normal.xz);
	}
}

void frx_endVertex(inout frx_VertexData data) {
	// NOOP
}
