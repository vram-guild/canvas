#include canvas:shaders/pipeline/options.glsl

/******************************************************
  canvas:shaders/pipeline/shadow.vert
******************************************************/

void frx_writePipelineVertex(in frx_VertexData data) {
	// move to camera origin
	data.vertex += frx_modelToCamera();

	vec4 cameraToCenter = vec4(frx_cameraView() * frx_viewDistance() * 0.5, 0.0);

	// move to center of camera frustum
	data.vertex -= cameraToCenter;

	// apply rotation
	data.vertex = frx_shadowViewMatrix() * data.vertex;

	// move towards sky light
	data.vertex += vec4(0.0, 0.0, -1.0 * frx_viewDistance(), 0.0);

	//vec4 viewCoord = frx_shadowViewMatrix() * data.vertex;
	gl_ClipVertex = data.vertex;
	gl_Position = frx_shadowProjectionMatrix() * data.vertex;
}
