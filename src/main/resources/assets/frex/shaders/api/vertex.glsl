#include frex:shaders/api/context.glsl

/******************************************************
  frex:shaders/api/vertex.glsl
******************************************************/

/**
 * Interpolated vertex position in camera space.
 * Set by renderer after material shader runs. Do not modify.
 */
out vec4 frx_vertex;

/**
 * Interpolated texture coordinate in mapped (non-normalized) coordinates.
 * Set by renderer after material shader runs. Do not modify.
 */
out vec2 frx_texcoord;

/**
 * Interpolated vertex color output.
 * Set by renderer after material shader runs. Do not modify.
 */
out vec4 frx_color;

#ifndef DEPTH_PASS
/**
 * Interpolated vertex normal in world/camera space.
 * Set by renderer after material shader runs. Do not modify.
 *
 * Not available in depth pass.
 */
out vec3 frx_normal;

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
 *
 * Not available in depth pass.
 */

/**
 * Interpolated camera distance. Used for fog.
 * Set by renderer after material shader runs. Do not modify.
 *
 * Not available in depth pass.
 */
out float frx_distance;

out vec4 frx_var0;
out vec4 frx_var1;
out vec4 frx_var2;
out vec4 frx_var3;

#endif

/*
 * Usage in API for material shaders:
 * 	  void frx_startVertex(inout frx_VertexData data)
 *
 * Passed to frx_startVertex at start of vertex shader processing
 * before any transformations are performed. Any changes made here
 * will affect render about. The primary use case is animation.
 *
 *
 * Usage in API for pipeline shaders:
 * 	  void frx_writeVertex(in frx_VertexData data)
 *
 * The renderer will marshal all data into the structure. (Vertex
 * formats are managed entirely by the renderer.) The renderer
 * also invokes the correct material shader.
 *
 * The renderer also manages variable state needed for atlas texture
 * transforms, material properties, and other elements of the FREX API.
 *
 * After the material shader runs, it will set values for frx_texcoord,
 * frx_vertex, frx_normal and frx_color.
 * The renderer does nothing else and calls frx_writeVertex when complete.
 *
 * The pipeline is responsible for ALL OTHER WRITES, including fog,
 * clip, vertex or any other built-in variable as required by Open GL.
 */
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
