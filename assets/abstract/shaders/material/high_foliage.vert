#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/lib/noise/noise3d.glsl

/******************************************************
  abstract:shaders/material/high_foliage.vert

  Based on "GPU-Generated Procedural Wind Animations for Trees"
  by Renaldas Zioma in GPU Gems 3, 2007
  https://developer.nvidia.com/gpugems/gpugems3/part-i-geometry/chapter-6-gpu-generated-procedural-wind-animations-trees
******************************************************/

void frx_materialVertex() {
#ifdef ANIMATED_FOLIAGE
	float globalWind = 0.2 + frx_rainGradient * 0.2;
	float t = frx_renderSeconds * 0.05;
	// 2.0 here is only diff from low version
	float wind = snoise(vec3((frx_vertex.xz + frx_modelToWorld.xz) * 0.0625, t)) * (2.0 - frx_texcoord.y) * globalWind;

	frx_vertex.x += (cos(t) * cos(t * 3) * cos(t * 5) * cos(t * 7) + sin(t * 25)) * wind;
	frx_vertex.z += sin(t * 19) * wind;
#endif
}
