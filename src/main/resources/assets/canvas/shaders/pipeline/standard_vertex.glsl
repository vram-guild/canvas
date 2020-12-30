#include canvas:shaders/internal/diffuse.glsl

/******************************************************
  canvas:shaders/pipeline/standard_vertex.glsl
******************************************************/

void frx_startPipelineVertex(inout frx_VertexData data) {
	vec4 viewCoord = gl_ModelViewMatrix * data.vertex;
	gl_ClipVertex = viewCoord;
	gl_FogFragCoord = length(viewCoord.xyz);
}

void frx_endtPipelineVertex(inout frx_VertexData data) {
	gl_Position = data.vertex;
		_cvv_texcoord = data.spriteUV;
		_cvv_color = data.color;
		_cvv_normal = data.normal;

	#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
		_cvv_diffuse = _cv_diffuse(_cvv_normal);
	#endif

		#if AO_SHADING_MODE != AO_MODE_NONE
		_cvv_ao = in_lightmap.b / 255.0;
	#endif

	_cvv_lightcoord = data.light;
}
