#include frex:shaders/api/vertex.glsl
#include frex:shaders/lib/face.glsl

/******************************************************
  abstract:shaders/material/lava.vert
******************************************************/

void frx_materialVertex() {
#ifndef DEPTH_PASS
	if (abs(frx_vertexNormal.y) < 0.001) {
	    frx_var0.xy = frx_faceUv(frx_vertex.xyz, frx_vertexNormal.xyz);
	    //must specify frx_var0.z explicitly on Nvidia cards
	    frx_var0.zw = vec2(0.0, 1.0);
	} else {
	    frx_var0.xy = frx_faceUv(frx_vertex.xyz, FACE_UP);
	    // apparently can't normalize (0, 0) on Nvidia cards
	    frx_var0.zw = abs(frx_vertexNormal.y)==1.0 ? vec2(0.0, 0.0) : -normalize(frx_vertexNormal.xz);
	}
#endif
}
