#include frex:shaders/wip/api/context.glsl
#include frex:shaders/wip/api/world.glsl

/******************************************************
  canvas:shaders/internal/diffuse.glsl
******************************************************/

/**
 * Formula mimics vanilla lighting for plane-aligned quads and is vaguely
 * consistent with Phong lighting ambient + diffuse for others.
 */
float _cv_diffuseBaked(vec3 normal) {
	// PERF: highly inefficient
	vec3 xVec = _cvu_normal_model_matrix * vec3(1.0, 0.0, 0.0);
	vec3 yVec = _cvu_normal_model_matrix * vec3(0.0, 1.0, 0.0);
	vec3 zVec = _cvu_normal_model_matrix * vec3(0.0, 0.0, 1.0);

	float x = dot(xVec, normal);
	float y = dot(yVec, normal);
	float z = dot(zVec, normal);

	return 0.5 + clamp(abs(x) * 0.1 + (y > 0 ? 0.5 * y : 0.0) + abs(z) * 0.3, 0.0, 0.5);
}

/**
 * Offers results similar to vanilla in Gui, assumes a fixed transform.
 */
float _cv_diffuseGui(vec3 normal) {
	// Note that vanilla rendering normally sends item models with raw colors and
	// canvas sends colors unmodified, so we do not need to compensate for any pre-buffer shading
	float light = 0.4
	+ 0.6 * clamp(dot(normal.xyz, vec3(-0.309, 0.927, -0.211)), 0.0, 1.0)
	+ 0.6 * clamp(dot(normal.xyz, vec3(0.518, 0.634, 0.574)), 0.0, 1.0);

	return min(light, 1.0);
}

/**
 * Unrotated, non-gui lights.  But not transformed into eye space.
 * Not sure how I want to do that yet.
 */
float _cv_diffuseWorld(vec3 normal) {
	float light = 0.4
	+ 0.6 * clamp(dot(normal.xyz, vec3(0.16169, 0.808452, -0.565916)), 0.0, 1.0)
	+ 0.6 * clamp(dot(normal.xyz, vec3(-0.16169, 0.808452, 0.565916)), 0.0, 1.0);

	return min(light, 1.0);
}

float _cv_diffuse (vec3 normal) {
	return _cv_diffuseBaked(normal);
}
