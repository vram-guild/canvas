#include frex:shaders/api/view.glsl

/******************************************************
  abstract:shaders/pipeline/shadow.vert
******************************************************/

uniform int frxu_cascade;

void frx_pipelineVertex() {
	// move to camera origin
	gl_Position = frx_shadowViewProjectionMatrix(frxu_cascade) * (frx_vertex + frx_modelToCamera);
}
