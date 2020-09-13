#include canvas:shaders/internal/header.glsl
#include frex:shaders/api/context.glsl
#include canvas:shaders/internal/varying.glsl
#include canvas:shaders/internal/vertex.glsl
#include canvas:shaders/internal/flags.glsl
#include frex:shaders/api/vertex.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/diffuse.glsl

//#include canvas:apitarget


/******************************************************
  canvas:shaders/internal/wip/wip.vert
******************************************************/

attribute vec4 in_color;
attribute vec2 in_uv;
attribute vec4 in_normal_flags;
attribute vec4 in_lightmap;
attribute vec2 in_material;

void main() {
	frx_VertexData data = frx_VertexData(
	gl_Vertex,
	in_uv,
	in_color,
	(in_normal_flags.xyz - 127.0) / 127.0,
	// Lightmap texture coorinates come in as 0-256.
	// Scale and offset slightly to hit center pixels
	// vanilla does this with a texture matrix
	in_lightmap.rg * 0.00390625 + 0.03125
	);

	// Adding +0.5 prevents striping or other strangeness in flag-dependent rendering
	// due to FP error on some cards/drivers.  Also made varying attribute invariant (rolls eyes at OpenGL)
	_cvv_flags = in_normal_flags.w + 0.5;


	//#ifdef _CV_HAS_VERTEX_START
	//frx_startVertex(data);
	//#endif

//	vec4 spriteBounds = texture2DLod(frxs_spriteInfo, vec2(0, in_material.x / _CV_SPRITE_INFO_TEXTURE_SIZE), 0);

	// snap sprite bounds to integer coordinates to correct for floating point error
//	spriteBounds *= vec4(_CV_ATLAS_WIDTH, _CV_ATLAS_HEIGHT, _CV_ATLAS_WIDTH, _CV_ATLAS_HEIGHT);
//	spriteBounds += vec4(0.5, 0.5, 0.5, 0.5);
//	spriteBounds -= fract(spriteBounds);
//	spriteBounds /= vec4(_CV_ATLAS_WIDTH, _CV_ATLAS_HEIGHT, _CV_ATLAS_WIDTH, _CV_ATLAS_HEIGHT);

//	data.spriteUV = spriteBounds.xy + data.spriteUV * spriteBounds.zw;
	data.spriteUV = _cv_textureCoord(data.spriteUV, 0);

	vec4 viewCoord = gl_ModelViewMatrix * data.vertex;
	gl_ClipVertex = viewCoord;
	gl_FogFragCoord = length(viewCoord.xyz);

	data.normal *= gl_NormalMatrix;
	data.vertex = gl_ModelViewProjectionMatrix * data.vertex;

	gl_Position = data.vertex;

	//#ifdef _CV_HAS_VERTEX_END
	//frx_endVertex(data);
	//#endif

	_cvv_texcoord = data.spriteUV;
	_cvv_color = data.color;
	_cvv_normal = data.normal;
	_cvv_ao = 1.0; //in_lightmap.b; // TODO: how to handle AO when not used?

	//#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
	_cvv_diffuse = _cv_diffuse(data.normal);
	//#endif

	//#ifndef CONTEXT_IS_GUI
	_cvv_lightcoord = data.light;
	//#endif

}
