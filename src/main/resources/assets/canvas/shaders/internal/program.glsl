#include frex:shaders/lib/bitwise.glsl
#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/vertex.glsl
#include frex:shaders/api/view.glsl

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

#define _CV_ARB_CONSERVATIVE_DEPTH 1 // We can't test the GL defintion GL_ARB_conservative_depth because it won't be present when our pre-processor runs.

	#ifdef _CV_ARB_CONSERVATIVE_DEPTH
layout (depth_unchanged) out float gl_FragDepth;
	#endif

	#ifndef _CV_VERTEX_DEFAULT
flat in vec4 _cv_modelToWorld;
flat in vec4 _cv_modelToCamera;
	#endif
#endif

#define _cv_vertexProgramId() _cvu_program.x
#define _cv_fragmentProgramId() _cvu_program.y
#define _cv_programDiscard() (_cvu_program.w == 0)

#ifdef VERTEX_SHADER
void _cv_setupProgram() {
	if (_cvu_context[_CV_ATLAS_WIDTH] == 0) {
		_cvu_program = texelFetch(_cvu_materialInfo, _CV_MATERIAL_ID);
		_cvu_program.w = _cv_testCondition(_cvu_program.w) ? 1 : 0;
		_cvv_spriteBounds = vec4(0.0, 0.0, 1.0, 1.0);
	} else {
		int i = _CV_MATERIAL_ID * 2;
		_cvu_program = texelFetch(_cvu_materialInfo, i);
		_cvu_program.w = (frx_isGui || _cv_testCondition(_cvu_program.w)) ? 1 : 0;
		_cvv_spriteBounds = vec4(texelFetch(_cvu_materialInfo, i + 1)) / 32768.0;
	}
}
#endif
