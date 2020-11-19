#include frex:shaders/api/vertex.glsl
#include frex:shaders/lib/face.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/api/world.glsl

/******************************************************
  canvas:shaders/material/water.vert
******************************************************/

float FBM(vec2 p, float t) {
    float n = 0.0;
    n += 0.50000 * frx_noise2dt(p * 1.0, t);
    n += 0.25000 * frx_noise2dt(p * 2.0, t);
    n += 0.12500 * frx_noise2dt(p * 4.0, t);
    n += 0.06250 * frx_noise2dt(p * 8.0, t);
    n += 0.03125 * frx_noise2dt(p * 16.0, t);
    return n / 0.984375;
}

void frx_startVertex(inout frx_VertexData data) {
	vec3 modelOrigin = frx_modelOriginWorldPos();

	if (abs(data.normal.y) < 0.001) {
	    frx_var0.xy = modelOrigin.xz + frx_faceUv(data.vertex.xyz, data.normal.xyz);
	    //must specify frx_var0.z explicitly on Nvidia cards
	    frx_var0.zw = vec2(0.0, 1.0);
	} else {
	    frx_var0.xy = modelOrigin.xz + frx_faceUv(data.vertex.xyz, FACE_UP);
	    // apparently can't normalize (0, 0) on Nvidia cards
	    frx_var0.zw = abs(data.normal.y)==1.0 ? vec2(0.0, 0.0) : -normalize(data.normal.xz);

	    if (data.normal.y > 0.99) {
	    	data.vertex.y += (FBM(frx_var0.xy, frx_renderSeconds()) - 0.5) * 0.2;
	    }
	}
}

void frx_endVertex(inout frx_VertexData data) {
	// NOOP
}
