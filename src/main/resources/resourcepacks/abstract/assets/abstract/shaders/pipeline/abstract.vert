#include frex:shaders/api/view.glsl
#include frex:shaders/api/player.glsl
#include abstract:handheld_light_config

/******************************************************
  abstract:shaders/pipeline/dev.vert
******************************************************/

#if HANDHELD_LIGHT_RADIUS != 0
flat out float _cvInnerAngle;
flat out float _cvOuterAngle;
out vec4 _cvViewVertex;
#endif

out vec4 shadowPos;

void frx_pipelineVertex() {
	if (frx_isGui) {
		gl_Position = frx_guiViewProjectionMatrix * frx_vertex;
		frx_distance = length(gl_Position.xyz);
	} else {
		frx_vertex += frx_modelToCamera;
		vec4 viewCoord = frx_viewMatrix * frx_vertex;
		frx_distance = length(viewCoord.xyz);
		gl_Position = frx_projectionMatrix * viewCoord;
#if HANDHELD_LIGHT_RADIUS != 0
		_cvViewVertex = viewCoord;
#endif

		shadowPos  = frx_shadowViewMatrix * frx_vertex;
	}

#if HANDHELD_LIGHT_RADIUS != 0
	_cvInnerAngle = sin(frx_heldLightInnerRadius);
	_cvOuterAngle = sin(frx_heldLightOuterRadius);
#endif
}
