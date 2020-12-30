#include frex:shaders/api/context.glsl

/******************************************************
  frex:shaders/api/vertex.glsl
******************************************************/

/*
 * Varying variables for generic use in material shaders. Material
 * shader authors are encouraged to exhaust these before creating new
 * custom out variables.
 *
 * This is necessary because custom shaders may be consolidated
 * into a single shader with logic controlled via uniforms or vertex data.
 * This is done either to reduce draw calls or as a way to achieve
 * sorted translucency with mixed custom shaders.
 *
 * If we do not reuse these variables, then three bad things can happen:
 *   1) Naming conflicts (could be avoided with care)
 *   2) Exceed hardware/driver limits
 *   3) Wasteful interpolation if unused varyings aren't stripped by the compiler.
 *
 * Authors do not need to worry about conflicting usage of these variables
 * by other shaders in the same compilation - only a single pair of custom
 * vertex/fragment shaders will be active for a single polygon.
 *
 * Note that pipeline shader devs should NOT use these.  There will only
 * ever be a single pipeline active at any time - piplines can define as
 * many out variables as needed, within reason.
 */
varying vec4 frx_var0;
varying vec4 frx_var1;
varying vec4 frx_var2;
varying vec4 frx_var3;

/**
 * Texture coordinate to intended to support special sampling use cases
 * during fragment shading.  WILL BE SET BY RENDERER.  Do not touch.
 */
varying vec2 frx_texcoord;

/*
 * Usage in API for material shaders:
 * 	  void frx_startVertex(inout frx_VertexData data)
 * 	  void frx_endVertex(inout frx_VertexData data)
 *
 * Passed to frx_startFragment at start of fragment shader processing
 * before any transformations are performed and passed
 * to frx_endVertex after all transformations are complete.
 *
 * Note that any changes made during cv_endVertex WILL affect
 * renderer output but changes here are generally tricky
 * and discouraged. The cv_endVertex call is meant for
 * retrieving post-transform values to set up varying variable.
 *
 * The exception to the above is vertex. See notes below.
 *
 *
 * Usage in API for pipeline shaders:
 * 	  void frx_startPipelineVertex(inout frx_VertexData data)
 * 	  void frx_endPipelineVertex(inout frx_VertexData data)
 *
 * The comments that apply to material shaders also apply here.
 * Both calls will come immediate after the material shader runs.
 *
 * The renderer will marshal all data into the structure. (Vertex
 * formats are managed entirely by the renderer.) Before the 2nd call,
 * the renderer will transform UV coordinates for atlas textures from normalized
 * to mapped and also apply matrix transforms to vertex and normal.
 *
 * The pipeline is responsible for ALL WRITES.  The renderer will
 * not update fog, clip, color, nor any other output variable.
 * Outputs that need pre-transform values should happen in frx_startPipelineVertex
 * and outpus that need post-transform values should happen in frx_endPipelineVertex.
 *
 * The renderer WILL set out variables for the interpolated values
 * presented in frx_startFragment but these are not directly exposed
 * outside of that method, except for frx_texcoord. Any other out variables
 * the pipeline needs must be set in one of these two methods.
 */
struct frx_VertexData {
	/*
	 * Vertex position. Transformation during cv_startVertex
	 * is the primary means for achieving animation effects.
	 * Remember that normals must be transformed separately!
	 *
	 * Will be world space during cv_startVertex and in
	 * screen space during cv_endVertex.
	 */
	vec4 vertex;

	/*
	 * The primary texture coordinate for the vertex.
	 * These are raw coordinates during cv_startVertex
	 * and will modified by the active texture matrix in
	 * cv_endVertex if one is in effect (usually not.)
	 *
	 * IMPORTANT: currently these are atlas coordinates.
	 * This will soon be changed to 0-1 coordinates within
	 * the texture itself. This will generally be more useful
	 * for in-shader effects that rely on position relative
	 * to texture bounds.
	 */
	vec2 spriteUV;

	/*
	 * RGB vertex color, with alpha.
	 * Usage of alpha is controlled by the material blend mode:
	 * 		if cutout is enabled, fragment will be discarded (see: api/material.glsl)
	 * 		if SHADER_PASS != SHADER_PASS_SOLID fragment will be blended using the alpha channel
	 *		in other cases (solid pass rendering) alpha is ignored and could be used for other purposes
	 *
	 * Canvas does not modify vertex alpha values given from the model
	 * in the solid render pass, even though the data is not used. This
	 * means mod authors can use that value for other purposes in the shader.
	 *
	 * Vertex color is not transformed by the renderer.
	 */
	vec4 color;

	/*
	 * Vertex normal. Transformation during cv_startVertex
	 * is the primary means for achieving animation effects.
	 * Transforming normal in addition to vertex is important
	 * for correct lighting.
	 *
	 * Will be in world space during cv_startVertex and in
	 * screen space during cv_endVertex.
	 */
	vec3 normal;

#ifdef VANILLA_LIGHTING
	/*
	 * Block and sky light intensity for this vertex as 0 to 1 values.
	 * Block is X and sky is Y.
	 *
	 * Depending on the context or lighting model in effect,
	 * this may not be populated or used.
	 *
	 * Avoid using or modifying this value unless VANILLA_LIGHTING is defined.
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
