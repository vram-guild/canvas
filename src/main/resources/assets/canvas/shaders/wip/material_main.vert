#include canvas:shaders/wip/header.glsl
#include frex:shaders/wip/api/context.glsl
#include canvas:shaders/wip/varying.glsl
#include canvas:shaders/wip/vertex.glsl
#include canvas:shaders/wip/flags.glsl
#include frex:shaders/wip/api/vertex.glsl
#include frex:shaders/wip/api/sampler.glsl
#include canvas:shaders/wip/diffuse.glsl
#include canvas:shaders/wip/program.glsl

void _cv_startVertex(inout frx_VertexData data, in int cv_programId) {
#include canvas:cv_start_vertex
}

void _cv_endVertex(inout frx_VertexData data, in int cv_programId) {
#include canvas:cv_end_vertex
}

/******************************************************
  canvas:shaders/internal/material_main.vert
******************************************************/

#define ATTRIB_COLOR
#ifdef ATTRIB_COLOR
attribute vec4 in_color;
#endif

#define ATTRIB_TEXTURE
#ifdef ATTRIB_TEXTURE
attribute vec2 in_uv;
#endif

#define ATTRIB_MATERIAL
#ifdef ATTRIB_MATERIAL
attribute vec2 in_material;
#endif

#define ATTRIB_LIGHTMAP
#ifdef ATTRIB_LIGHTMAP
attribute vec4 in_lightmap;
#endif

#define ATTRIB_NORMAL
#ifdef ATTRIB_NORMAL
attribute vec4 in_normal_flags;
#endif

void main() {
	frx_VertexData data = frx_VertexData(
	gl_Vertex,

#ifdef ATTRIB_TEXTURE
	in_uv,
#else
	vec2(0.0, 0.0),
#endif

#ifdef ATTRIB_COLOR
	in_color,
#else
	vec4(1.0, 1.0, 1.0, 1.0),
#endif

#ifdef ATTRIB_NORMAL
	(in_normal_flags.xyz - 127.0) / 127.0,
#else
	vec4(0.0, 1.0, 0.0, 0.0)
#endif

#ifdef ATTRIB_LIGHTMAP
	in_lightmap.rg * 0.00390625 + 0.03125
#else
	vec4(1.0, 1.0, 1.0, 1.0)
#endif
	);

	// Adding +0.5 prevents striping or other strangeness in flag-dependent rendering
	// due to FP error on some cards/drivers.  Also made varying attribute invariant (rolls eyes at OpenGL)
	_cvv_flags = in_normal_flags.w + 0.5;

	int cv_programId = _cv_programId();
	_cv_startVertex(data, cv_programId);

#ifdef ATTRIB_MATERIAL
	if (_cvu_material[_CV_SPRITE_INFO_TEXTURE_SIZE] != 0.0) {
		// for sprite atlas textures, convert from normalized (0-1) to interpolated coordinates
		vec4 spriteBounds = texture2DLod(frxs_spriteInfo, vec2(0, in_material.x / _cvu_material[_CV_SPRITE_INFO_TEXTURE_SIZE]), 0);

		float atlasHeight = _cvu_material[_CV_ATLAS_HEIGHT];
		float atlasWidth = _cvu_material[_CV_ATLAS_WIDTH];

		// snap sprite bounds to integer coordinates to correct for floating point error
		spriteBounds *= vec4(atlasWidth, atlasHeight, atlasWidth, atlasHeight);
		spriteBounds += vec4(0.5, 0.5, 0.5, 0.5);
		spriteBounds -= fract(spriteBounds);
		spriteBounds /= vec4(atlasWidth, atlasHeight, atlasWidth, atlasHeight);

		data.spriteUV = spriteBounds.xy + data.spriteUV * spriteBounds.zw;
	}
#endif

	data.spriteUV = _cv_textureCoord(data.spriteUV, 0);

	vec4 viewCoord = gl_ModelViewMatrix * data.vertex;
	gl_ClipVertex = viewCoord;
	gl_FogFragCoord = length(viewCoord.xyz);

	//#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
	_cvv_diffuse = _cv_diffuseBaked(data.normal);
	//#endif

	data.normal *= gl_NormalMatrix;
	data.vertex = gl_ModelViewProjectionMatrix * data.vertex;

	gl_Position = data.vertex;

	_cv_endVertex(data, cv_programId);

	_cvv_texcoord = data.spriteUV;
	_cvv_color = data.color;
	_cvv_normal = data.normal;
	_cvv_ao = in_lightmap.b;


	//#ifndef CONTEXT_IS_GUI
	_cvv_lightcoord = data.light;
	//#endif

}
