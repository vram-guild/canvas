#include canvas:shaders/internal/hd/hd.glsl

/******************************************************
  canvas:shaders/internal/hd/hd.frag
******************************************************/

vec4 aoFactor(vec2 lightCoord) {
// Don't apply AO for item renders
#if CONTEXT_IS_BLOCK

	vec4 hd = texture2D(u_utility, v_hd_lightmap);
	float ao = hd.r;

	#if ENABLE_LIGHT_NOISE
		vec4 dither = texture2D(u_dither, gl_FragCoord.xy / 16.0);
		ao += dither.r / 16.0 - (1.0 / 32.0);
	#endif

    #if AO_SHADING_MODE == AO_MODE_SUBTLE_BLOCK_LIGHT || AO_SHADING_MODE == AO_MODE_SUBTLE_ALWAYS
        // accelerate the transition from 0.4 (should be the minimum) to 1.0
        float bao = (ao - 0.4) / 0.6;
        bao = clamp(bao, 0.0, 1.0);
        bao = 1.0 - bao;
        bao = bao * bao * (1.0 - lightCoord.x * 0.6);
        bao = 0.4 + (1.0 - bao) * 0.6;

        #if AO_SHADING_MODE == AO_MODE_SUBTLE_ALWAYS
            return vec4(bao, bao, bao, 1.0);
        #else
            ao = mix(ao, bao, lightCoord.x);
            return vec4(ao, ao, ao, 1.0);
        #endif
    #else
        return vec4(ao, ao, ao, 1.0);
    #endif
#else
    return vec4(1.0, 1.0, 1.0, 1.0);
#endif
}


vec2 lightCoord() {
#if ENABLE_SMOOTH_LIGHT
    vec4 hd = texture2D(u_utility, v_hd_lightmap);
    // PERF: return directly vs extra math below
    vec2 lightCoord = vec2(hd.g, hd.a) * 15.0;

    #if ENABLE_LIGHT_NOISE
        vec4 dither = texture2D(u_dither, gl_FragCoord.xy / 8.0);
        lightCoord += dither.r / 64.0 - (1.0 / 128.0);
    #endif

    return (lightCoord + 0.5) / 16.0;
#else
    return v_lightcoord;
#endif
}
