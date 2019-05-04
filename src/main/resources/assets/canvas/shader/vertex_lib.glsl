attribute vec4 in_normal_ao;

#ifndef WHITE_0
    attribute vec4 in_color_0;
#endif

attribute vec2 in_uv_0;
attribute vec4 in_lightmap;

#ifdef ENABLE_SMOOTH_LIGHT
attribute vec2 in_hd_lightmap;
#endif

#if LAYER_COUNT > 1
attribute vec4 in_color_1;
attribute vec2 in_uv_1;
#endif

#if LAYER_COUNT > 2
attribute vec4 in_color_2;
attribute vec2 in_uv_2;
#endif

vec2 textureCoord(vec2 coordIn, int matrixIndex) {
	vec4 temp = gl_TextureMatrix[matrixIndex] * coordIn.xyxy;
	return temp.xy;
}

vec3 diffuseNormal(vec4 viewCoord, vec3 normal) {
#if CONTEXT == CONTEXT_ITEM_WORLD
    // TODO: Need to transform normals for in-world items to get directionally correct shading.
    // Problem is that we don't have a MVM for the lights. Will need to capture that
    // or transform the lights on CPU side, which is probably the better deal.
    return normal;
#else
    return normal;
#endif
}

void setupVertex() {
    gl_Position = ftransform();

    vec4 viewCoord = gl_ModelViewMatrix * gl_Vertex;
    gl_ClipVertex = viewCoord;
    gl_FogFragCoord = length(viewCoord.xyz);
    v_texcoord_0 = textureCoord(in_uv_0, 0);

#ifdef CONTEXT_IS_BLOCK
    v_ao = (in_normal_ao.w + 1.0) * 0.5;
#endif

#ifdef ENABLE_SMOOTH_LIGHT
    v_hd_lightmap = in_hd_lightmap + 0.5;
//#ifdef ENABLE_LIGHT_NOISE
    //v_noisecoord = uv(gl_Vertex.xyz, in_normal_ao.xyz);
#endif

    v_diffuse = diffuse(diffuseNormal(viewCoord, in_normal_ao.xyz));

#if CONTEXT != CONTEXT_ITEM_GUI
    // the lightmap texture matrix is scaled to 1/256 and then offset + 8
    // it is also clamped to repeat and has linear min/mag
    v_lightcoord = in_lightmap.rg * 0.00390625 + 0.03125;
#endif


    // Fixes Acuity #5
    // Adding +0.5 prevents striping or other strangeness in flag-dependent rendering
    // due to FP error on some cards/drivers.  Also made varying attribute invariant (rolls eyes at OpenGL)
    v_flags =  in_lightmap.ba + 0.5;

#ifdef WHITE_0
    v_color_0 = vec4(1.0, 1.0, 1.0, 1.0);
#else
    v_color_0 = in_color_0;
#endif

#if LAYER_COUNT > 1
    v_color_1 = in_color_1;
    v_texcoord_1 = textureCoord(in_uv_1, 0);
#endif

#if LAYER_COUNT > 2
    v_color_2 = in_color_2;
    v_texcoord_2 = textureCoord(in_uv_2, 0);
#endif
}

