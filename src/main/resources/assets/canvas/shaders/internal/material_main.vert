#include frex:shaders/api/header.glsl
#include frex:shaders/api/context.glsl
#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/varying.glsl
#include canvas:shaders/internal/flags.glsl
#include canvas:shaders/internal/vertex.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/material_main.vert
******************************************************/
uniform usamplerBuffer _cvu_spriteInfo;

void _cv_startVertex(inout frx_VertexData data, in int cv_programId) {
#include canvas:startvertex
}

void main() {
#ifdef VANILLA_LIGHTING
	frx_VertexData data = frx_VertexData(
		vec4(in_vertex, 1.0),
		in_uv,
		in_color,
		(in_normal_flags.xyz - 127.0) / 127.0,
		in_lightmap.rg * 0.00390625 + 0.03125,
		in_lightmap.b / 255.0
	);
#else
	frx_VertexData data = frx_VertexData(
		vec4(in_vertex, 1.0),
		in_uv,
		in_color,
		(in_normal_flags.xyz - 127.0) / 127.0
	);
#endif

	_cv_setupProgram();
	_cvv_flags = uint(_cvu_program.z);
	int cv_programId = _cv_vertexProgramId();

	// map texture coordinates
	if (_cvu_context[_CV_ATLAS_WIDTH] == 0.0) {
		_cvv_spriteBounds = vec4(0.0, 0.0, 1.0, 1.0);

	} else {
		_cvv_spriteBounds = vec4(texelFetch(_cvu_spriteInfo, in_sprite)) / 32768.0;
	}

	// material shaders go first
	_cv_startVertex(data, cv_programId);

	frx_texcoord = frx_mapNormalizedUV(data.spriteUV);
	frx_color = data.color;
	frx_normal = data.normal;
	frx_vertex = data.vertex;

	// pipeline shader handles additional writes/out variables
	frx_writePipelineVertex(data);
}
