#include frex:shaders/api/context.glsl
#include frex:shaders/lib/bitwise.glsl
#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/vertex.glsl

/******************************************************
  canvas:shaders/internal/program.glsl
******************************************************/

#define _CV_MATERIAL_INFO_TEXTURE_SIZE 0
#define _CV_MAX_SHADER_COUNT 0

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

bool _cv_programDiscard() {
	return false;
}

#else

uniform sampler2D _cvu_materialInfo;

flat varying ivec4 _cvu_program;

int _cv_vertexProgramId() {
	return _cvu_program.x;
}

int _cv_fragmentProgramId() {
	return _cvu_program.y;
}

bool _cv_programDiscard() {
	return _cvu_program.w == 0;
}

#endif

#define PROGRAM_FLAG_GUI 0

float _cv_isGui() {
	return frx_bitValue(uint(_cvu_program.z), PROGRAM_FLAG_GUI);
}

#ifdef VERTEX_SHADER
void _cv_setupProgram() {
#ifndef PROGRAM_BY_UNIFORM
	float materialIndex = in_material.y;
	float y = floor((materialIndex + 0.1) / _CV_MATERIAL_INFO_TEXTURE_SIZE);
	float x = materialIndex - (y * _CV_MATERIAL_INFO_TEXTURE_SIZE);
	vec2 coord = vec2(x, y);

	vec4 raw = texture2DLod(_cvu_materialInfo, (coord + 0.5) / _CV_MATERIAL_INFO_TEXTURE_SIZE, 0);
	_cvu_program = ivec4(raw * vec4(_CV_MAX_SHADER_COUNT, _CV_MAX_SHADER_COUNT, 1.0, 1.0));
	_cvu_program.w = _cv_testCondition(_cvu_program.w) ? 1 : 0;
#endif
}
#endif
