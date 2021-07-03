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
#ifdef CV_VF
	vec4 inputColor = texelFetch(_cvu_vfColor, in_color);
#else
	vec4 inputColor = in_color;
#endif

	frx_VertexData data = frx_VertexData(
		vec4(in_vertex, 1.0),
		in_uv,
		inputColor,
		in_normal
	);

	_cv_setupProgram();
	_cvv_flags = uint(_cvu_program.z);

	// material shaders go first
	_cv_startVertex(data, _cv_vertexProgramId());

	frx_texcoord = frx_mapNormalizedUV(data.spriteUV);
	frx_color = data.color;
	frx_vertex = data.vertex;

	// pipeline shader handles additional writes/out variables
	frx_writePipelineVertex(data);
}
