#include frex:shaders/api/context.glsl

/******************************************************
  frex:shaders/api/fragment.glsl
******************************************************/

/*
 * Varying variables for generic use. See comments in vertex.glsl.
 */
varying vec4 frx_var0;
varying vec4 frx_var1;
varying vec4 frx_var2;
varying vec4 frx_var3;

/**
 * Texture coordinate from vertex.glsl.  Normally not needed because
 * the renderer handles primary texture sampling in order to populate
 * the data structure before the material shader runs.  Exposed to
 * support exotic sampling use cases.
 */
varying vec2 frx_texcoord;

/*
 * Usage in API for material shaders:
 * 	  void frx_startFragment(inout frx_FragmentData fragData)
 *
 * Passed to frx_startFragment at start of fragment shader processing
 * before any computations or derivations are performed.
 *
 *
 * Usage in API for pipeline shaders:
 * 	  frx_FragmentData frx_createPipelineFragment()
 * 	  void frx_writePipelineFragment(in frx_FragmentData fragData)
 *
 * The renderer will test for discard caused by material condition. If
 * discard does not happen, it will call frx_createPipelineFragment() and the
 * pipeline shader must create and populate the data structure with data written
 * during frx_writePipelineVertex().
 *
 * The renderer will then call the material shader and test again for conditional
 * rendering and cutout - discarding fragments if appropriate.
 *
 * Lastly, the renderer calls frx_writePipelineFragment() which must update
 * and targets needed for render output or additional passes.
 *
 * The pipeline shader is responsible for ALL WRITES.
 * The renderer will not update depth or any color attachment.
 */
struct frx_FragmentData {
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

#ifdef VANILLA_LIGHTING
	/*
	 * Block and sky light intensity for this fragment.
	 * Block is X and sky is Y.
	 * Encoding may depend on renderer configuration.
	 *
	 * Depending on the context or lighting model in effect, this
	 * may be an interpolated vertex value, a value from
	 * a texture lookup, or it may not be populated or used.
	 *
	 * Will not be available unless VANILLA_LIGHTING is defined.
	 *
	 * The emissive flag is generally a better alternative.
	 */
	vec2 light;

	/*
	 * AO shading value from CPU lighting. 0 to 1.
	 *
	 * Depending on the context or lighting model in effect,
	 * this may not be populated or used.
	 *
	 * Avoid using or modifying this value unless VANILLA_LIGHTING is defined.
	 */
	float aoShade;
#endif
};
