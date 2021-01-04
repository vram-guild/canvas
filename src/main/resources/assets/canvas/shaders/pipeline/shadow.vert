#include canvas:shaders/pipeline/options.glsl

/******************************************************
  canvas:shaders/pipeline/shadow.vert
******************************************************/

void frx_writePipelineVertex(in frx_VertexData data) {
	if (frx_modelOriginType() == MODEL_ORIGIN_SCREEN) {
		vec4 viewCoord = gl_ModelViewMatrix * data.vertex;
		gl_ClipVertex = viewCoord;
		gl_Position = gl_ProjectionMatrix * viewCoord;
	} else {
		data.vertex += frx_modelToCamera();
		vec4 viewCoord = frx_viewMatrix() * data.vertex;
		gl_ClipVertex = viewCoord;
		gl_Position = frx_projectionMatrix() * viewCoord;
	}
}
