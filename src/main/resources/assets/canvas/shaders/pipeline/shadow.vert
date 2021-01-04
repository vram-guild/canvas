#include canvas:shaders/pipeline/options.glsl

/******************************************************
  canvas:shaders/pipeline/shadow.vert
******************************************************/

void frx_writePipelineVertex(in frx_VertexData data) {
	data.vertex += frx_modelToCamera();
	vec4 viewCoord = frx_shadowViewMatrix() * data.vertex;
	gl_ClipVertex = viewCoord;
	gl_Position = frx_shadowProjectionMatrix() * viewCoord;

}
