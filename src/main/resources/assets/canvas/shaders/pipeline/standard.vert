#include canvas:shaders/pipeline/diffuse.glsl
#include canvas:shaders/pipeline/varying.glsl
#include frex:shaders/api/view.glsl
#include frex:shaders/api/player.glsl
#include canvas:handheld_light_config

/******************************************************
  canvas:shaders/pipeline/standard.vert
******************************************************/

#if HANDHELD_LIGHT_RADIUS != 0
flat out float _cvInnerAngle;
flat out float _cvOuterAngle;
out vec4 _cvViewVertex;
#endif

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

	if (frx_isGui()) {
		gl_Position = frx_guiViewProjectionMatrix() * data.vertex;
		frx_distance = length(gl_Position.xyz);
	} else {
		data.vertex += frx_modelToCamera();
		vec4 viewCoord = frx_viewMatrix() * data.vertex;
		frx_distance = length(viewCoord.xyz);
		gl_Position = frx_projectionMatrix() * viewCoord;
#if HANDHELD_LIGHT_RADIUS != 0
		_cvViewVertex = viewCoord;
#endif
	}

#if HANDHELD_LIGHT_RADIUS != 0
	_cvInnerAngle = sin(frx_heldLightInnerRadius());
	_cvOuterAngle = sin(frx_heldLightOuterRadius());
#endif

#ifdef VANILLA_LIGHTING
	pv_lightcoord = data.light;
	pv_ao = data.aoShade;
#endif

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
	pv_diffuse = p_diffuse(data.normal);
#endif
}
