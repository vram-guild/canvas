/******************************************************
  canvas:shaders/api/fragment.glsl
******************************************************/

/*
 * Usage in API:
 * 	  void cv_startFragment(inout cv_FragmentData fragData)
 *
 * Passed to cv_startFragment at start of fragment shader processing
 * before any computations or derivations are performed.
 */
struct cv_FragmentData {
	/*
	 * RGB color from primary texture, with alpha.
	 *
	 * Usage of alpha is controlled by the material blend mode:
	 * 		if cutout is enabled, fragment will be discarded (see: api/material.glsl)
	 * 		if SHADER_PASS != SHADER_PASS_SOLID fragment will be blended using the alpha channel
	 *		in other cases (solid pass rendering) alpha is ignored and could be used for other purposes
	 *
	 *	Will be multiplied by vertexColor (below) to derive fragment base color before shading.
	 */
	vec4 spriteColor;

	/*
	 * RGB color interpolated from vertex color, with alpha.
	 * Usage of alpha is the same as for spriteColor - above.
	 *
	 * Canvas does not modify vertex alpha values given from the model
	 * in the solid render pass, even though the data is not used. This
	 * means mod authors can use that value for other purposes in the shader.
	 *
	 * Will be multiplied by spriteColor (above) to derive fragment base color before shading.
	 */
	vec4 vertexColor;

	/*
	 * Emissivity of this fragment is emissive. Currently this will be 1.0
	 * if the model material is marked emissive before buffering and zero otherwise.
	 *
	 * Future enhancements will allow for emissive texture maps also.
	 */
	float emissivity;

	/*
	 * True if the this fragment should receive diffuse shading.
	 * Typical use is to disable directional shading on emissve surfaces.
	 *
	 * Note this may be ignored with non-vanilla lighting models and settings.
	 */
	bool diffuse;

	/*
	 * True if the this fragment should receive ambient occlusion shading.
	 * Typical use is to disable ao shading on emissve surfaces.
	 *
	 * Note this may be ignored with non-vanilla lighting models and settings.
	 */
	bool ao;

	/*
	 * Interpolated vertex normal for this fragment.
	 * Renderer will use this to compute lighting.
	 *
	 * Future enhancements will allow for normal texture maps also.
	 */
	vec3 vertexNormal;

	/*
	 * Block and sky light intensity for this fragment as 0 to 1 values.
	 * Block is X and sky is Y.
	 *
	 * Depending on the context or lighting model in effect, this
	 * may be an interpolated vertex value, a value from
	 * a texture lookup, or it may not be populated or used.
	 *
	 * Recommendation is to avoid using or modifying this value
	 * unless VANILLA_LIGHTING = TRUE.
	 *
	 * The emissive flag is generally a better alternative.
	 */
	vec2 light;
};
