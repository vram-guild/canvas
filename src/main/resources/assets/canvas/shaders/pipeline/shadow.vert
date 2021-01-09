/******************************************************
  canvas:shaders/pipeline/shadow.vert
******************************************************/

void frx_writePipelineVertex(in frx_VertexData data) {
	// move to camera origin
	data.vertex += frx_modelToCamera();

	// apply rotation
	data.vertex = frx_shadowViewMatrix() * data.vertex;

	gl_ClipVertex = data.vertex;
	gl_Position = frx_shadowProjectionMatrix() * data.vertex;
}
