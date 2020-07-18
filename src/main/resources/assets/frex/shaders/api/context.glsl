#include canvas:shaders/lib/constant.glsl

/******************************************************
  canvas:shaders/api/context.glsl

  Definitions to inicate operating mode of renderer.
  Typical usage is to control conditional compilation of
  features that may not work or work differently in,
  for example, GUI vs world rendering.
******************************************************/

// true if this is a block context
#define CONTEXT_IS_BLOCK FALSE

// true if this is an item context
#define CONTEXT_IS_ITEM FALSE

// true if this is a GUI context
#define CONTEXT_IS_GUI FALSE

#define SHADER_PASS_SOLID 		0
#define SHADER_PASS_DECAL 		1
#define SHADER_PASS_TRANSLUCENT	2
#define SHADER_PASS_PROCESS		3

#define SHADER_PASS SHADER_PASS_SOLID

#define SHADER_TYPE_VERTEX 		0
#define SHADER_TYPE_FRAGMENT 	1

#define SHADER_TYPE SHADER_TYPE_VERTEX

// When set to false, lightmaps and other vanilla-specific data may not be valid or present
#define VANILLA_LIGHTING TRUE
