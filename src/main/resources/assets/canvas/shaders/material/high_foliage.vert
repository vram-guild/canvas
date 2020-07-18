#include canvas:shaders/api/vertex.glsl
#include canvas:shaders/api/world.glsl
#include canvas:shaders/lib/math.glsl
#include frex:shaders/lib/noise/noise3d.glsl

/******************************************************
  canvas:shaders/material/high_foliage.vert
******************************************************/

void cv_startVertex(inout cv_VertexData data) {
	float rain = cv_rainGradient();
	float globalWind = 0.2 + rain * 0.2;
	float t = cv_renderSeconds() * 0.05;
	vec3 modelOrigin = cv_modelOriginWorldPos();
																			 // 2.0 here is only diff from low version
	float wind = snoise(vec3((data.vertex.xz + modelOrigin.xz) * 0.0625, t)) * (2.0 - data.spriteUV.y) * globalWind;

	data.vertex.x += (cos(t) * cos(t * 3) * cos(t * 5) * cos(t * 7) + sin(t * 25)) * wind;
	data.vertex.z += sin(t * 19) * wind;
}

