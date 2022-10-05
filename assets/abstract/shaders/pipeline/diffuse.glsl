#include frex:shaders/api/world.glsl
#include frex:shaders/api/view.glsl
#include abstract:basic_light_config

/******************************************************
  abstract:shaders/pipeline/diffuse.glsl
******************************************************/

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
#ifdef VERTEX_SHADER
	out float pv_diffuse;
#else
	in float pv_diffuse;
#endif
#endif

const vec3 LIGHT1      = vec3 ( 0.104196384,  0.947239857, -0.303116754);
const vec3 LIGHT2      = vec3 (-0.104196384,  0.947239857,  0.303116754);
const vec3 LIGHT2_DARK = vec3 (-0.104196384, -0.947239857,  0.303116754);

/**
 * Formula mimics vanilla lighting for plane-aligned quads and is vaguely
 * consistent with Phong lighting ambient + diffuse for others.
 */
float p_diffuseBaked(vec3 normal) {
	// in nether underside is lit like top
	vec3 secondaryVec = frx_worldIsSkyDarkened == 1 ? LIGHT2_DARK : LIGHT2;

	float l1 = max(0.0, dot(LIGHT1, normal));
	float l2 = max(0.0, dot(secondaryVec, normal));

	return 0.5 + min(0.5, l1 + l2);
}

// for testing - not a good way to do it
float p_diffuseSky(vec3 normal) {
	float f = dot(frx_skyLightVector, normal);
	f = f > 0.0 ? 0.4 * f : 0.2 * f;
	return 0.6 + frx_skyLightTransitionFactor * f;
}

/**
 * Offers results similar to vanilla in GUI, assumes a fixed transform.
 */
float p_diffuseGui(vec3 normal) {
	normal = normalize(normal);
	float light = 0.4
	+ 0.6 * clamp(dot(normal.xyz, vec3(-0.93205774, 0.26230583, -0.24393857)), 0.0, 1.0)
	+ 0.6 * clamp(dot(normal.xyz, vec3(-0.10341814, 0.9751613, 0.18816751)), 0.0, 1.0);
	return min(light, 1.0);
}

float p_diffuse (vec3 normal) {
	return frx_isGui ? p_diffuseGui(normal) : p_diffuseBaked(normal);
}
