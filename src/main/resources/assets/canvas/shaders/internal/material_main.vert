#include frex:shaders/api/header.glsl
#include frex:shaders/api/context.glsl
#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/varying.glsl
#include canvas:shaders/internal/flags.glsl
#include canvas:shaders/internal/vertex.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/material_main.vert
******************************************************/
uniform sampler2D _cvu_spriteInfo;

void _cv_startVertex(inout frx_VertexData data, in int cv_programId) {
#include canvas:startvertex
}

void main() {
#ifdef VANILLA_LIGHTING
	frx_VertexData data = frx_VertexData(
		gl_Vertex,
		in_uv,
		in_color,
		(in_normal_flags.xyz - 127.0) / 127.0,
		in_lightmap.rg * 0.00390625 + 0.03125,
		in_lightmap.b / 255.0
	);
#else
	frx_VertexData data = frx_VertexData(
		gl_Vertex,
		in_uv,
		in_color,
		(in_normal_flags.xyz - 127.0) / 127.0
	);
#endif

	// Adding +0.5 prevents striping or other strangeness in flag-dependent rendering
	// due to FP error on some cards/drivers.  Also made varying attribute invariant (rolls eyes at OpenGL)
	_cvv_flags = uint(in_normal_flags.w + 0.5);
	_cv_setupProgram();
	int cv_programId = _cv_vertexProgramId();

	// map texture coordinates
	if (_cvu_context[_CV_SPRITE_INFO_TEXTURE_SIZE] == 0.0) {
		_cvv_spriteBounds = vec4(0.0, 0.0, 1.0, 1.0);

	} else {
		float spriteIndex = in_material.x;
		// for sprite atlas textures, convert from normalized (0-1) to interpolated coordinates
		vec4 spriteBounds = texture2DLod(_cvu_spriteInfo, vec2(0, spriteIndex / _cvu_context[_CV_SPRITE_INFO_TEXTURE_SIZE]), 0);

		float atlasHeight = _cvu_context[_CV_ATLAS_HEIGHT];
		float atlasWidth = _cvu_context[_CV_ATLAS_WIDTH];

		// snap sprite bounds to integer coordinates to correct for floating point error
		spriteBounds *= vec4(atlasWidth, atlasHeight, atlasWidth, atlasHeight);
		spriteBounds += vec4(0.5, 0.5, 0.5, 0.5);
		spriteBounds -= fract(spriteBounds);
		spriteBounds /= vec4(atlasWidth, atlasHeight, atlasWidth, atlasHeight);
		_cvv_spriteBounds = spriteBounds;
	}

	// material shaders go first
	_cv_startVertex(data, cv_programId);

	frx_texcoord = frx_mapNormalizedUV(data.spriteUV);
	frx_color = data.color;
	frx_normal = data.normal;
	frx_vertex = data.vertex;

	// pipeline shader handles additional writes/out variables
	frx_writePipelineVertex(data);
}
