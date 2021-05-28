#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/pipeline/varying.glsl
******************************************************/

#ifdef VANILLA_LIGHTING
#ifdef VERTEX_SHADER
	out vec2 pv_lightcoord;
	out float pv_ao;
#else
	in vec2 pv_lightcoord;
	in float pv_ao;
#endif
#endif
