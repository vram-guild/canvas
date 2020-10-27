/******************************************************
  canvas:shaders/internal/program.glsl
******************************************************/

// undefine to use vertex data for program selection
#define PROGRAM_BY_UNIFORM

#ifdef PROGRAM_BY_UNIFORM

uniform ivec2 _cvu_program;

int _cv_vertexProgramId() {
	return _cvu_program.x;
}

int _cv_fragmentProgramId() {
	return _cvu_program.y;
}

#else

varying vec2 _cvu_program;

int _cv_vertexProgramId() {
	return int(_cvu_program.x);
}

int _cv_fragmentProgramId() {
	return int(_cvu_program.y);
}

#endif
