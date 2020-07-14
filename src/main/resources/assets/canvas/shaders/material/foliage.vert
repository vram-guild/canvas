#include canvas:shaders/api/vertex.glsl
#include canvas:shaders/api/world.glsl

/******************************************************
  canvas:shaders/material/default.vert
******************************************************/

#define hash(p) fract(mod(p.x, 1.0) * 73758.23f - p.y)

void cv_startVertex(inout cv_VertexData data) {
	float rand_ang = hash(data.vertex.xz);
	float time = cv_renderSeconds();
	float rainStrength = 0.5;
	float maxStrength = 1.0;
	float reset = cos(rand_ang * 10.0 + time * 0.1);
	reset = max( reset * reset, max(rainStrength, 0.1));
	data.vertex.x += (sin(rand_ang * 10.0 + time) * 0.2) * (reset * maxStrength) * (1.0 - data.spriteUV.y);
}
