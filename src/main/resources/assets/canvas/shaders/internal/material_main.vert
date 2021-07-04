#include frex:shaders/api/header.glsl
#include frex:shaders/api/context.glsl
#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/flags.glsl
#include canvas:shaders/internal/vertex.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/material_main.vert
******************************************************/

void _cv_startVertex(inout frx_VertexData data, in int cv_programId) {
#include canvas:startvertex
}

void main() {
	_cv_prepareForVertex();

#ifdef VANILLA_LIGHTING
	frx_VertexData data = frx_VertexData(
		vec4(in_vertex, 1.0),
		in_uv,
		in_color,
		in_normal,
		clamp(in_lightmap.rg * 0.00390625, 0.03125, 0.96875),
		in_ao
	);
#else
	frx_VertexData data = frx_VertexData(
		vec4(in_vertex, 1.0),
		in_uv,
		in_color,
		in_normal
	);
#endif

	_cv_setupProgram();
	_cvv_flags = uint(_cvu_program.z);

	// material shaders go first
	_cv_startVertex(data, _cv_vertexProgramId());

	frx_texcoord = frx_mapNormalizedUV(data.spriteUV);
	frx_color = data.color;
	frx_normal = data.normal;
	frx_vertex = data.vertex;

	// pipeline shader handles additional writes/out variables
	frx_writePipelineVertex(data);
}
