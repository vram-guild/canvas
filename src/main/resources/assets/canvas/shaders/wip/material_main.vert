#include canvas:shaders/wip/header.glsl
#include frex:shaders/wip/api/context.glsl
#include canvas:shaders/wip/varying.glsl
#include canvas:shaders/wip/vertex.glsl
#include canvas:shaders/wip/flags.glsl
#include frex:shaders/wip/api/vertex.glsl
#include frex:shaders/wip/api/sampler.glsl
#include canvas:shaders/wip/diffuse.glsl
#include canvas:shaders/wip/program.glsl

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

attribute vec4 in_color;
attribute vec2 in_uv;
attribute vec4 in_lightmap;
attribute vec4 in_normal_flags;

vec3 _cv_decodeNormal(vec2 raw) {
	raw = (raw - 127.0) / 127.0;
	float z = 1.0 - (raw.x * raw.x) - (raw.y - raw.y);
	return vec3(raw.xy, sqrt(z));
}

void main() {
	frx_VertexData data = frx_VertexData(
	gl_Vertex,
	in_uv,
	in_color,
	_cv_decodeNormal(in_normal_flags.xy),
	in_lightmap.rg * 0.00390625 + 0.03125
	);

	// Adding +0.5 prevents striping or other strangeness in flag-dependent rendering
	// due to FP error on some cards/drivers.  Also made varying attribute invariant (rolls eyes at OpenGL)
	_cvv_flags = in_lightmap.w + 0.5;

	int cv_programId = _cv_vertexProgramId();
	_cv_startVertex(data, cv_programId);

	if (_cvu_material[_CV_SPRITE_INFO_TEXTURE_SIZE] != 0.0) {
		float spriteIndex = in_normal_flags.w * 256.0 + in_normal_flags.z;
		// for sprite atlas textures, convert from normalized (0-1) to interpolated coordinates
		vec4 spriteBounds = texture2DLod(frxs_spriteInfo, vec2(0, spriteIndex / _cvu_material[_CV_SPRITE_INFO_TEXTURE_SIZE]), 0);

		float atlasHeight = _cvu_material[_CV_ATLAS_HEIGHT];
		float atlasWidth = _cvu_material[_CV_ATLAS_WIDTH];

		// snap sprite bounds to integer coordinates to correct for floating point error
		spriteBounds *= vec4(atlasWidth, atlasHeight, atlasWidth, atlasHeight);
		spriteBounds += vec4(0.5, 0.5, 0.5, 0.5);
		spriteBounds -= fract(spriteBounds);
		spriteBounds /= vec4(atlasWidth, atlasHeight, atlasWidth, atlasHeight);

		data.spriteUV = spriteBounds.xy + data.spriteUV * spriteBounds.zw;
	}

	data.spriteUV = _cv_textureCoord(data.spriteUV, 0);

	vec4 viewCoord = gl_ModelViewMatrix * data.vertex;
	gl_ClipVertex = viewCoord;
	gl_FogFragCoord = length(viewCoord.xyz);

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
	_cvv_diffuse = _cv_diffuseBaked(data.normal);
#endif

	data.normal *= gl_NormalMatrix;
	data.vertex = gl_ModelViewProjectionMatrix * data.vertex;

	gl_Position = data.vertex;

	_cv_endVertex(data, cv_programId);

	_cvv_texcoord = data.spriteUV;
	_cvv_color = data.color;
	_cvv_normal = data.normal;

#if AO_SHADING_MODE != AO_MODE_NONE
	_cvv_ao = in_lightmap.b;
#endif

	_cvv_lightcoord = data.light;
}
