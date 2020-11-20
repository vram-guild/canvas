#include frex:shaders/lib/bitwise.glsl
#include canvas:shaders/internal/vertex.glsl
#include frex:shaders/api/sampler.glsl

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

#else
flat varying vec3 _cvu_program;

int _cv_vertexProgramId() {
	return int(_cvu_program.x);
}

int _cv_fragmentProgramId() {
	return int(_cvu_program.y);
}

#endif

#define PROGRAM_FLAG_GUI 0

float _cv_isGui() {
	return frx_bitValue(int(_cvu_program.z), PROGRAM_FLAG_GUI);
}

#ifdef VERTEX_SHADER
void _cv_setupProgram() {
#ifndef PROGRAM_BY_UNIFORM
	float materialIndex = in_material.y;
	float y = floor((materialIndex + 0.1) / _CV_MATERIAL_INFO_TEXTURE_SIZE);
	float x = materialIndex - (y * _CV_MATERIAL_INFO_TEXTURE_SIZE);
	vec2 coord = vec2(x, y);

	_cvu_program = texture2DLod(frxs_materialInfo, (coord + 0.5) / _CV_MATERIAL_INFO_TEXTURE_SIZE, 0).xyz;
	_cvu_program *= vec3(_CV_MAX_SHADER_COUNT, _CV_MAX_SHADER_COUNT, 1.0);
#endif
}
#endif
