#include frex:shaders/api/context.glsl
#include frex:shaders/lib/bitwise.glsl
#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/vertex.glsl

/******************************************************
  canvas:shaders/internal/program.glsl
******************************************************/

#define _CV_ATLAS_WIDTH 0
#define _CV_ATLAS_HEIGHT 1
#define _CV_TARGET_INDEX 2

uniform int[3] _cvu_context;

#define _CV_MAX_SHADER_COUNT 0

uniform isamplerBuffer _cvu_materialInfo;

#ifdef VERTEX_SHADER
	flat out ivec4 _cvu_program;
#else
	flat in ivec4 _cvu_program;
#endif

int _cv_vertexProgramId() {
	return _cvu_program.x;
}

int _cv_fragmentProgramId() {
	return _cvu_program.y;
}

bool _cv_programDiscard() {
	return _cvu_program.w == 0;
}



#ifdef VERTEX_SHADER
void _cv_setupProgram() {
	_cvu_program = texelFetch(_cvu_materialInfo, in_material);
	_cvu_program.w = _cv_testCondition(_cvu_program.w) ? 1 : 0;
}
#endif
