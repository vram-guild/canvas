#include frex:shaders/api/context.glsl
#include frex:shaders/api/world.glsl
#include canvas:shaders/internal/program.glsl

/******************************************************
  canvas:shaders/internal/diffuse.glsl
******************************************************/

/**
 * Formula mimics vanilla lighting for plane-aligned quads and is vaguely
 * consistent with Phong lighting ambient + diffuse for others.
 */
float _cv_diffuseBaked(vec3 normal) {
	mat3 normalModelMatrix = frx_normalModelMatrix();
	vec3 lv1 = normalize(normalModelMatrix * vec3(0.1, 1.0, -0.3));

	// in nether underside is lit like top
	vec3 secondaryVec = frx_isSkyDarkened() ? vec3(-0.1, -1.0, 0.3) : vec3(-0.1, 1.0, 0.3);
	vec3 lv2 = normalize(normalModelMatrix * secondaryVec);

	float l1 = max(0.0, dot(lv1, normal));
	float l2 = max(0.0, dot(lv2, normal));

	return 0.4 + min(0.6, l1 + l2);
}

/**
 * Offers results similar to vanilla in GUI, assumes a fixed transform.
 * Vanilla GUI light setup looks like this:
 *
 * light(GL_LIGHT0, GL_POSITION, -0.96104145, -0.078606814, -0.2593495, 0
 * light(GL_LIGHT0, GL_DIFFUSE, getBuffer(0.6F, 0.6F, 0.6F, 1.0F));
 * light(GL_LIGHT0, GL_AMBIENT, getBuffer(0.0F, 0.0F, 0.0F, 1.0F));
 * light(GL_LIGHT0, GL_SPECULAR, getBuffer(0.0F, 0.0F, 0.0F, 1.0F));
 *
 * light(GL_LIGHT1, GL_POSITION, -0.26765957, -0.95667744, 0.100838766, 0
 * light(GL_LIGHT1, GL_DIFFUSE, getBuffer(0.6F, 0.6F, 0.6F, 1.0F));
 * light(GL_LIGHT1, GL_AMBIENT, getBuffer(0.0F, 0.0F, 0.0F, 1.0F));
 * light(GL_LIGHT1, GL_SPECULAR, getBuffer(0.0F, 0.0F, 0.0F, 1.0F));
 *
 * glShadeModel(GL_FLAT);
 * glLightModel(GL_LIGHT_MODEL_AMBIENT, 0.4F, 0.4F, 0.4F, 1.0F);
 */
float _cv_diffuseGui(vec3 normal) {
	normal = normalize(gl_NormalMatrix * normal);
	float light = 0.4
	+ 0.6 * clamp(dot(normal.xyz, vec3(-0.96104145, -0.078606814, -0.2593495)), 0.0, 1.0)
	+ 0.6 * clamp(dot(normal.xyz, vec3(-0.26765957, -0.95667744, 0.100838766)), 0.0, 1.0);
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
	return _cv_isGui() == 1.0 ? _cv_diffuseGui(normal) : _cv_diffuseBaked(normal);
}
