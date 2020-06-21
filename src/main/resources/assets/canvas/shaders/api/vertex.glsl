/******************************************************
  canvas:shaders/api/vertex.glsl
******************************************************/

/*
 * Usage in API:
 * 	  void cv_startVertex(inout cv_VertexData data)
 * 	  void cv_endVertex(inout cv_VertexData data)
 *
 * Passed to cv_startFragment at start of fragment shader processing
 * before any transformations are performed and passed
 * to cv_endVertex after all transformations are complete.
 *
 * Note that any changes made during cv_endVertex WILL affect
 * renderer output but changes here are generally tricky
 * and discouraged. The cv_endVertex call is meant for
 * retrieving post-transform values to set up varying variable.
 *
 * The exception to the above is vertex. See notes below.
 */
struct cv_VertexData {
	/*
	 * Vertex position. Transformation during cv_startVertex
	 * is the primary means for achieving animation effects.
	 * Remember that normals must be transformed separately!
	 *
	 * Will be world space during cv_startVertex and in
	 * screen space during cv_endVertex.
	 *
	 * Note that unlike other attributes, changes made in
	 * cv_endVertex will NOT be used.  This limitation may
	 * be removed in a future enhancement.
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
	vec4 vertexColor;

	/*
	 * Vertex normal. Transformation during cv_startVertex
	 * is the primary means for achieving animation effects.
	 * Transforming normal in addition to vertex is important
	 * for correct lighting.
	 *
	 * Will be in world space during cv_startVertex and in
	 * screen space during cv_endVertex.
	 */
	vec3 vertexNormal;

	/*
	 * Block and sky light intensity for this vertex as 0 to 1 values.
	 * Block is X and sky is Y.
	 *
	 * Depending on the context or lighting model in effect,
	 * this may not be populated or used.
	 *
	 * Recommendation is to avoid using or modifying this value
	 * unless VANILLA_LIGHTING = TRUE.
	 *
	 * The emissive flag is generally a better alternative.
	 */
	vec2 light;
};
