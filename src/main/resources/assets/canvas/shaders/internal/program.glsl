#include frex:shaders/lib/bitwise.glsl

/******************************************************
  canvas:shaders/internal/program.glsl
******************************************************/

// undefine to use vertex data for program selection
#define PROGRAM_BY_UNIFORM

#ifdef PROGRAM_BY_UNIFORM

uniform ivec3 _cvu_program;

int _cv_vertexProgramId() {
	return _cvu_program.x;
}

int _cv_fragmentProgramId() {
	return _cvu_program.y;
}

#else

varying vec3 _cvu_program;

int _cv_vertexProgramId() {
	return int(_cvu_program.x);
}

int _cv_fragmentProgramId() {
	return int(_cvu_program.y);
}

#endif

#define PROGRAM_FLAG_GUI 0

float _cv_isGui() {
	return frx_bitValue(_cvu_program.z, PROGRAM_FLAG_GUI);
}
