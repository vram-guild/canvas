#include frex:shaders/api/view.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/api/player.glsl
#include abstract:handheld_light_config

/******************************************************
  abstract:shaders/pipeline/dev.vert
******************************************************/

#if HANDHELD_LIGHT_RADIUS != 0
flat out float _cvInnerAngle;
flat out float _cvOuterAngle;
out vec3 _cvViewVertex;
out vec3 _cvWorldVertex;
#endif

out vec4 shadowPos;

void frx_pipelineVertex() {
	if (frx_isGui) {
		gl_Position = frx_guiViewProjectionMatrix * frx_vertex;
		_cvWorldVertex = (frx_vertex.xyz * frx_normalModelMatrix) * 0.2 + frx_cameraPos;
		frx_distance = length(gl_Position.xyz);
	} else {
		frx_vertex += frx_modelToCamera;
		_cvWorldVertex = frx_vertex.xyz + frx_cameraPos + frx_normal * 0.05;
		vec4 viewCoord = frx_viewMatrix * frx_vertex;
		frx_distance = length(viewCoord.xyz);
		gl_Position = frx_projectionMatrix * viewCoord;
#if HANDHELD_LIGHT_RADIUS != 0
		_cvViewVertex = viewCoord.xyz;
#endif

		shadowPos  = frx_shadowViewMatrix * frx_vertex;
	}

#if HANDHELD_LIGHT_RADIUS != 0
	_cvInnerAngle = sin(frx_heldLightInnerRadius);
	_cvOuterAngle = sin(frx_heldLightOuterRadius);
#endif
}
