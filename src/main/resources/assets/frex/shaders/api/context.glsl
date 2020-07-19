/******************************************************
  frex:shaders/api/context.glsl

  Definitions to inicate operating mode of renderer.
  Typical usage is to control conditional compilation of
  features that may not work or work differently in,
  for example, GUI vs world rendering.
******************************************************/

// present if this is a block context
//#define CONTEXT_IS_BLOCK

// present if this is an item context
//#define CONTEXT_IS_ITEM

// present if this is a GUI context
//#define CONTEXT_IS_GUI

#define SHADER_PASS_SOLID 		0
#define SHADER_PASS_DECAL 		1
#define SHADER_PASS_TRANSLUCENT	2
#define SHADER_PASS_PROCESS		3

#define SHADER_PASS SHADER_PASS_SOLID

#define SHADER_TYPE_VERTEX 		0
#define SHADER_TYPE_FRAGMENT 	1

#define SHADER_TYPE SHADER_TYPE_VERTEX

// If not present, lightmaps and other vanilla-specific data may not be valid or present
#define VANILLA_LIGHTING

// present in world context only when feature is enabled - if not present then foliage shaders should NOOP
#define ANIMATED_FOLIAGE
