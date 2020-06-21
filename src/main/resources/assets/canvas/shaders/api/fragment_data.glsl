/******************************************************
  canvas:shaders/api/fragment_data.glsl
******************************************************/

/*
 * Usage in API:
 * 	  void cv_startFragment(inout cv_FragmentInput inputData)
 *
 * Passed to cv_startFragment at start of fragment shader processing
 * before any computations or derivations are performed.
 */
struct cv_FragmentInput {
	/**
	 * RGB color from primary texture, with alpha.
	 *
	 * Usage of alpha is controlled by the material blend mode:
	 * 		if cutout is enabled, fragment will be discarded (see: api/material.glsl)
	 * 		if
	 *
	 */
	vec4 spriteColor;
	vec4 vertexColor;
	bool emissive;
	bool diffuse;

	vec3 vertexNormal;
};

struct cv_FragmentOutput {
	vec4 baseColor;
	bool emissive;
	vec3 normal;
};
