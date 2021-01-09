/******************************************************
  canvas:shaders/pipeline/shadow.vert
******************************************************/

void frx_writePipelineVertex(in frx_VertexData data) {
	// move to camera origin
	vec4 pos = data.vertex + frx_modelToCamera();

	gl_ClipVertex = frx_shadowViewMatrix() * pos;
	gl_Position = frx_shadowViewProjectionMatrix() * pos;
}
