/******************************************************
  canvas:shaders/internal/varying.glsl
******************************************************/

#ifdef VERTEX_SHADER
flat out vec4 _cvv_spriteBounds;
#else
flat in vec4 _cvv_spriteBounds;
#endif
