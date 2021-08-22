/****************************************************************
 * frex:shaders/api/vertex.glsl - Canvas Implementation
 ***************************************************************/

out vec4 frx_vertex;
out vec2 frx_texcoord;
out vec4 frx_vertexColor;

#ifndef DEPTH_PASS
out vec3 frx_vertexNormal;
out vec4 frx_vertexLight;
out vec4 frx_var0;
out vec4 frx_var1;
out vec4 frx_var2;
out vec4 frx_var3;
out float frx_distance;
#endif

struct frx_VertexData {
	/*
	 * Vertex position in camera space. Transformation in frx_startVertex
	 * is the primary means for achieving animation effects.
	 * Remember that normals must be transformed separately!
	 */
	vec4 vertex;

	/*
	 * The primary texture coordinate for the vertex.
	 * These are always normalized 0-1 coordinates in frx_startVertex.
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
	 * Vertex color is not transformed by the renderer. Pipeline
	 * implementations should honor this contract.
	 */
	vec4 color;

	/*
	 * Vertex normal in camera/world space. Transformation in frx_startVertex
	 * is the primary means for achieving animation effects.
	 * Transforming normal in addition to vertex is important
	 * for correct lighting.
	 */
	vec3 normal;

#ifndef DEPTH_PASS
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
	 *
	 * Not available in depth pass.
	 */
	vec2 light;

	/*
	 * AO shading value from CPU lighting. 0 to 1.
	 *
	 * Depending on the context or lighting model in effect,
	 * this may not be populated or used.
	 *
	 * Avoid using or modifying this value unless VANILLA_LIGHTING is defined.
	 *
	 * Not available in depth pass.
	 */
	float aoShade;
#endif
#endif
};
