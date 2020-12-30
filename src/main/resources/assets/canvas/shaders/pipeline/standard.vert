#include canvas:shaders/pipeline/options.glsl
#include canvas:shaders/pipeline/diffuse.glsl

/******************************************************
  canvas:shaders/pipeline/standard.vert
******************************************************/

void frx_startPipelineVertex(inout frx_VertexData data) {
	vec4 viewCoord = gl_ModelViewMatrix * data.vertex;
	gl_ClipVertex = viewCoord;
	gl_FogFragCoord = length(viewCoord.xyz);
}

void frx_endPipelineVertex(inout frx_VertexData data) {
	gl_Position = data.vertex;

	#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
		_cvv_diffuse = _cv_diffuse(data.normal);
	#endif
}
