/******************************************************
  canvas:shaders/internal/program.glsl
******************************************************/

// undefine to use vertex data for program selection
#define PROGRAM_BY_UNIFORM

#ifdef PROGRAM_BY_UNIFORM

uniform int _cvu_program;

int _cv_programId() {
	return _cvu_program;
}

#else

varying float _cvu_programId;

int _cv_programId() {
	return int(_cvu_programId);
}

#endif
