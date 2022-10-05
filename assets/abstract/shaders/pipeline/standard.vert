#include abstract:shaders/pipeline/diffuse.glsl
#include frex:shaders/api/view.glsl
#include frex:shaders/api/player.glsl
#include abstract:handheld_light_config

/******************************************************
  abstract:shaders/pipeline/standard.vert
******************************************************/

#if HANDHELD_LIGHT_RADIUS != 0
flat out float _cvInnerAngle;
flat out float _cvOuterAngle;
out vec4 _cvViewVertex;
#endif

void frx_pipelineVertex() {
	if (frx_isGui) {
		gl_Position = frx_guiViewProjectionMatrix * frx_vertex;
		frx_distance = length(gl_Position.xyz);
#if HANDHELD_LIGHT_RADIUS != 0
		_cvViewVertex = gl_Position;
#endif
	} else {
		frx_vertex += frx_modelToCamera;
		vec4 viewCoord = frx_viewMatrix * frx_vertex;
		frx_distance = length(viewCoord.xyz);
		gl_Position = frx_projectionMatrix * viewCoord;
#if HANDHELD_LIGHT_RADIUS != 0
		_cvViewVertex = viewCoord;
#endif
	}

#if HANDHELD_LIGHT_RADIUS != 0
	_cvInnerAngle = sin(frx_heldLightInnerRadius);
	_cvOuterAngle = sin(frx_heldLightOuterRadius);
#endif

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
	pv_diffuse = p_diffuse(frx_vertexNormal);
#endif
}
