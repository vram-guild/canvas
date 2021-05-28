/******************************************************
  canvas:shaders/internal/hd/hd.glsl
******************************************************/

uniform sampler2D u_utility;

#ifdef VERTEX_SHADER
	flat out vec2 v_hd_lightmap;
#else
	flat in vec2 v_hd_lightmap;
#endif

#define FLAG_VERTEX_LIGHT       5
