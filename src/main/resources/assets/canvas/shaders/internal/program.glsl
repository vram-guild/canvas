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
#define _CV_CONTEXT_FLAGS 3

#define _CV_CONTEXT_FLAG_HAND 0

uniform int[4] _cvu_context;

#define _CV_MAX_SHADER_COUNT 0

uniform isamplerBuffer _cvu_materialInfo;

#ifdef VERTEX_SHADER
	flat out ivec4 _cvu_program;
	flat out vec4 _cvv_spriteBounds;

	// UGLY: _cv_modelOrigin is in vertex.glsl due to include order
#else
	flat in ivec4 _cvu_program;
	flat in vec4 _cvv_spriteBounds;

	#ifndef _CV_VERTEX_DEFAULT
	flat in vec4 _cv_modelToWorld;
	flat in vec4 _cv_modelToCamera;
	#endif
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
	if (_cvu_context[_CV_ATLAS_WIDTH] == 0) {
		_cvu_program = texelFetch(_cvu_materialInfo, in_material);
		_cvu_program.w = _cv_testCondition(_cvu_program.w) ? 1 : 0;
		_cvv_spriteBounds = vec4(0.0, 0.0, 1.0, 1.0);
	} else {
		int i = in_material * 2;
		_cvu_program = texelFetch(_cvu_materialInfo, i);
		_cvu_program.w = _cv_testCondition(_cvu_program.w) ? 1 : 0;
		_cvv_spriteBounds = vec4(texelFetch(_cvu_materialInfo, i + 1)) / 32768.0;
	}
}
#endif
