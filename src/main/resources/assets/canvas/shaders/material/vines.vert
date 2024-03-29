#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/lib/noise/noise4d.glsl

/******************************************************
  canvas:shaders/material/vines.vert

  Based on "GPU-Generated Procedural Wind Animations for Trees"
  by Renaldas Zioma in GPU Gems 3, 2007
  https://developer.nvidia.com/gpugems/gpugems3/part-i-geometry/chapter-6-gpu-generated-procedural-wind-animations-trees
******************************************************/

#define NOISE_SCALE 0.125

void frx_materialVertex() {
	#ifdef ANIMATED_FOLIAGE
	float globalWind = 0.2 + frx_rainGradient * 0.2;

	// wind gets stronger higher in the world
	globalWind *= (0.5 + smoothstep(64.0, 255.0, frx_vertex.y));

	float t = frx_renderSeconds * 0.05;

	vec3 pos = (frx_vertex.xyz + frx_modelToWorld.xyz) * NOISE_SCALE;
	float wind = snoise(vec4(pos, t)) * globalWind;

	// the only difference with leaves is clamping
	vec2 minPos = floor(frx_vertex.xz) + vec2(0.01);
	vec2 maxPos = ceil(frx_vertex.xz) - vec2(0.01);

	frx_vertex.x += (cos(t) * cos(t * 3) * cos(t * 5) * cos(t * 7) + sin(t * 25)) * wind;
	frx_vertex.z += sin(t * 19) * wind;

	// clamp based on normals
	frx_vertex.xz = mix(frx_vertex.xz, clamp(frx_vertex.xz, minPos, maxPos), abs(frx_vertexNormal.xz));
	#endif
}
