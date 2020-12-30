#include canvas:shaders/internal/header.glsl
#include frex:shaders/api/context.glsl
#include canvas:shaders/internal/varying.glsl
#include canvas:shaders/internal/vertex.glsl
#include canvas:shaders/internal/flags.glsl
#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/program.glsl
#include canvas:shaders/pipeline/standard_vertex.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/material_main.vert
******************************************************/

void _cv_startVertex(inout frx_VertexData data, in int cv_programId) {
#include canvas:startvertex
}

void _cv_endVertex(inout frx_VertexData data, in int cv_programId) {
#include canvas:endvertex
}

void main() {
	frx_VertexData data = frx_VertexData(
		gl_Vertex,
		in_uv,
		in_color,
		(in_normal_flags.xyz - 127.0) / 127.0,
		in_lightmap.rg * 0.00390625 + 0.03125
	);

	// Adding +0.5 prevents striping or other strangeness in flag-dependent rendering
	// due to FP error on some cards/drivers.  Also made varying attribute invariant (rolls eyes at OpenGL)
	_cvv_flags = uint(in_normal_flags.w + 0.5);

	_cv_setupProgram();

	int cv_programId = _cv_vertexProgramId();

	// material shaders go first
	_cv_startVertex(data, cv_programId);

	// pipeline shader goes after
	// will typically do writes or set out variables here that use pre-transform values
	frx_startPipelineVertex(data);

	// map texture coordinates
	if (_cvu_atlas[_CV_SPRITE_INFO_TEXTURE_SIZE] != 0.0) {
		float spriteIndex = in_material.x;
		// for sprite atlas textures, convert from normalized (0-1) to interpolated coordinates
		vec4 spriteBounds = texture2DLod(frxs_spriteInfo, vec2(0, spriteIndex / _cvu_atlas[_CV_SPRITE_INFO_TEXTURE_SIZE]), 0);

		float atlasHeight = _cvu_atlas[_CV_ATLAS_HEIGHT];
		float atlasWidth = _cvu_atlas[_CV_ATLAS_WIDTH];

		// snap sprite bounds to integer coordinates to correct for floating point error
		spriteBounds *= vec4(atlasWidth, atlasHeight, atlasWidth, atlasHeight);
		spriteBounds += vec4(0.5, 0.5, 0.5, 0.5);
		spriteBounds -= fract(spriteBounds);
		spriteBounds /= vec4(atlasWidth, atlasHeight, atlasWidth, atlasHeight);

		data.spriteUV = spriteBounds.xy + data.spriteUV * spriteBounds.zw;
	}

	data.spriteUV = _cv_textureCoord(data.spriteUV, 0);

	// apply transforms
	//data.normal *= gl_NormalMatrix;
	data.vertex = gl_ModelViewProjectionMatrix * data.vertex;

	// post-transform material shaders
	_cv_endVertex(data, cv_programId);

	// pipeline shader handles additional writes/out variables
	frx_endtPipelineVertex(data);
}
