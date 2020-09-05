#include frex:shaders/api/context.glsl
#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/lib/noise/noise3d.glsl

/******************************************************
  canvas:shaders/material/high_foliage.vert

  Based on "GPU-Generated Procedural Wind Animations for Trees"
  by Renaldas Zioma in GPU Gems 3, 2007
  https://developer.nvidia.com/gpugems/gpugems3/part-i-geometry/chapter-6-gpu-generated-procedural-wind-animations-trees
******************************************************/

void frx_startVertex(inout frx_VertexData data) {
	#ifdef ANIMATED_FOLIAGE
	float rain = frx_rainGradient();
	float globalWind = 0.2 + rain * 0.2;
	float t = frx_renderSeconds() * 0.05;
	vec3 modelOrigin = frx_modelOriginWorldPos();
	// 2.0 here is only diff from low version
	float wind = snoise(vec3((data.vertex.xz + modelOrigin.xz) * 0.0625, t)) * (2.0 - data.spriteUV.y) * globalWind;

	data.vertex.x += (cos(t) * cos(t * 3) * cos(t * 5) * cos(t * 7) + sin(t * 25)) * wind;
	data.vertex.z += sin(t * 19) * wind;
	#endif
}

