#include canvas:shaders/pipeline/varying.glsl
#include frex:shaders/api/view.glsl

/******************************************************
  canvas:shaders/pipeline/dev.vert
******************************************************/

out vec4 shadowPos;

void frx_writePipelineVertex(in frx_VertexData data) {
	// WIP: remove - various api tests
	//if (data.vertex.z < frx_viewDistance() * -0.25) {
	//	data.color = vec4(0.0, 0.0, 0.0, 1.0);
	//}

	// world pos consistency test
	//vec3 worldPos = data.vertex.xyz + frx_modelOriginWorldPos();
	//float f = fract(worldPos.x / 32.0);
	//data.color = vec4(f, f, f, 1.0);

	// apply transforms
	//data.normal *= gl_NormalMatrix;

	data.vertex += frx_modelToCamera();
	vec4 viewCoord = frx_viewMatrix() * data.vertex;
	frx_distance = length(viewCoord.xyz);
	gl_Position = frx_projectionMatrix() * viewCoord;

	shadowPos  = frx_shadowViewMatrix() * data.vertex;

	pv_lightcoord = data.light;
	pv_ao = data.aoShade;
}
