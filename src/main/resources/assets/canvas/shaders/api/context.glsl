#include canvas:shaders/lib/constant.glsl

/******************************************************
  canvas:shaders/api/context.glsl
******************************************************/

// true if this is a block context
#define CONTEXT_IS_BLOCK FALSE

// true if this is an item context
#define CONTEXT_IS_ITEM FALSE

// true if this is a GUI context
#define CONTEXT_IS_GUI FALSE

#define SHADER_TYPE_VERTEX 		0
#define SHADER_TYPE_FRAGMENT 	1

#define SHADER_TYPE SHADER_TYPE_VERTEX

// When set to false, lightmaps and other vanilla-specific data may not be valid or present
#define VANILLA_LIGHTING = TRUE

uniform sampler2D _cvu_textures;
uniform sampler2D _cvu_lightmap;
uniform vec4 _cvu_emissiveColor;
uniform vec3 _cvu_eye_position;
