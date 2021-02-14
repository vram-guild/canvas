// updated to 130 when not running on a mac
#version 120
// removed when not running on a mac
#extension GL_EXT_gpu_shader4 : enable

#define VERTEX_SHADER

// mac doesn't understand uint syntax
#if __VERSION__ < 130
	#define uint unsigned int
#else
	#ifdef VERTEX_SHADER
		#define varying out
		#define attribute in
	#else
		#define varying in
	#endif
#endif
