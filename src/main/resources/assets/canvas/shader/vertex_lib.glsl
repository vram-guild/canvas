//attribute vec4 in_normal_ao;
attribute vec4 in_color_0;
attribute vec2 in_uv_0;
attribute vec4 in_lightmap;

#if LAYER_COUNT > 1
attribute vec4 in_color_1;
attribute vec2 in_uv_1;
#endif

#if LAYER_COUNT > 2
attribute vec4 in_color_2;
attribute vec2 in_uv_2;
#endif

void setupVertex()
{
    gl_Position = u_modelViewProjection * gl_Vertex;
    vec4 viewCoord = u_modelView * gl_Vertex;
    gl_ClipVertex = viewCoord;
    gl_FogFragCoord = length(viewCoord.xyz);
    v_texcoord_0 = in_uv_0;

    // the lightmap texture matrix is scaled to 1/256 and then offset + 8
    // it is also clamped to repeat and has linear min/mag
    v_light = texture2D(u_lightmap, (in_lightmap.rg * 0.00367647) + 0.03125);

    // Fixes Acuity #5
    // Adding +0.5 prevents striping or other strangeness in flag-dependent rendering
    // due to FP error on some cards/drivers.  Also made varying attribute invariant (rolls eyes at OpenGL)
    v_flags =  in_lightmap.b + 0.5;

    v_color_0 = in_color_0;

#if LAYER_COUNT > 1
    v_color_1 = in_color_1; //vec4(in_color_1.rgb * shade, in_color_1.a);
    v_texcoord_1 = in_uv_1;
#endif

#if LAYER_COUNT > 2
    v_color_2 = in_color_2; //vec4(in_color_2.rgb * shade, in_color_2.a);
    v_texcoord_2 = in_uv_2;
#endif
}

