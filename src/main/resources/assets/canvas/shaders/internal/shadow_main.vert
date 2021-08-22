#include frex:shaders/api/header.glsl
#define DEPTH_PASS
#include frex:shaders/api/context.glsl
#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/flags.glsl
#include canvas:shaders/internal/vertex.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/shadow_main.vert
******************************************************/

void _cv_startVertex(inout frx_VertexData data, in int cv_programId) {
#include canvas:startvertex
}

void main() {
	_cv_prepareForVertex();
	frx_vertex = vec4(in_vertex, 1.0);
	frx_texcoord = in_uv;
	frx_vertexColor = in_color;
	
	_cv_setupProgram();
	_cvv_flags = uint(_cvu_program.z);

	// material shaders go first
	_cv_startVertex(data, _cv_vertexProgramId());

	frx_texcoord = frx_mapNormalizedUV(frx_texcoord);

	// pipeline shader handles additional writes/out variables
	frx_pipelineVertex();
}
