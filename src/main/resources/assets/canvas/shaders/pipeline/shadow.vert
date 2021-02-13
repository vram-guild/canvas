#include frex:shaders/api/view.glsl

/******************************************************
  canvas:shaders/pipeline/shadow.vert
******************************************************/

void frx_writePipelineVertex(in frx_VertexData data) {
	// move to camera origin
	vec4 pos = data.vertex + frx_modelToCamera();
	gl_Position = frx_shadowViewProjectionMatrix(0) * pos;
}
