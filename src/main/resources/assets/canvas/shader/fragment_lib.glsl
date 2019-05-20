vec4 colorAndLightmap(vec4 fragmentColor,  int layerIndex, vec4 light) {
    return bitValue(v_flags.x, layerIndex) == 0 ? light * fragmentColor : u_emissiveColor * fragmentColor;
}

vec4 aoFactor(vec2 lightCoord) {
// Don't apply AO for item renders
#if CONTEXT_IS_BLOCK
    #if ENABLE_SMOOTH_LIGHT
        vec4 aotex = texture2D(u_utility, v_hd_ao);
        float ao = aotex.r;
    #else
        float ao = v_ao;
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

float effectModifier() {
    return u_world[WORLD_EFFECT_MODIFIER];
}

vec2 lightCoord() {
#if ENABLE_SMOOTH_LIGHT
    vec4 block = texture2D(u_utility, v_hd_blocklight);
    vec4 sky = texture2D(u_utility, v_hd_skylight);
    // PERF: return directly vs extra math below
    vec2 lightCoord = vec2(block.r, sky.r) * 15.0;

    #if ENABLE_LIGHT_NOISE
        vec4 dither = texture2D(u_dither, gl_FragCoord.xy / 8.0);
        lightCoord += dither.r / 64.0 - (1.0 / 128.0);
    #endif

    return (lightCoord + 0.5) / 16.0;
#else
    return v_lightcoord;
#endif
}

vec4 diffuseColor() {

    #if CONTEXT != CONTEXT_ITEM_GUI
        vec2 lightCoord = lightCoord();
    #endif

    #if CONTEXT_IS_BLOCK
        vec4 light = texture2D(u_lightmap, lightCoord);
    #elif CONTEXT == CONTEXT_ITEM_GUI
        vec4 light = vec4(1.0, 1.0, 1.0, 1.0);
    #else
        vec4 light = texture2D(u_lightmap, v_lightcoord);
    #endif

    #if HARDCORE_DARKNESS
        if(u_world[WORLD_HAS_SKYLIGHT] == 1.0 && u_world[WORLD_NIGHT_VISION] == 0.0) {
            float floor = u_world[WOLRD_MOON_SIZE] * lightCoord.y;
            float dark = 1.0 - smoothstep(0.0, 0.8, 1.0 - luminance(light.rgb));
            dark = max(floor, dark);
            light *= vec4(dark, dark, dark, 1.0);
        }
    #endif

    #if AO_SHADING_MODE != AO_MODE_NONE && CONTEXT_IS_BLOCK
        vec4 aoFactor = aoFactor(lightCoord);
    #endif

    #if DIFFUSE_SHADING_MODE == DIFFUSE_MODE_SKY_ONLY && CONTEXT_IS_BLOCK
        vec4 diffuse;
        if(u_world[WORLD_HAS_SKYLIGHT] == 1.0 && u_world[WORLD_NIGHT_VISION] == 0) {
            float d = 1.0 - v_diffuse;
            d *= u_world[WORLD_EFFECTIVE_INTENSITY];
            d *= lightCoord.y;
            d += 0.03125;
            d = clamp(1.0 - d, 0.0, 1.0);
            diffuse = vec4(d, d, d, 1.0);
        } else {
            diffuse = vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
        }

    #elif DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
        vec4 diffuse = vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
    #endif

    #if CONTEXT == CONTEXT_BLOCK_SOLID
        float non_mipped_0 = bitValue(v_flags.x, FLAG_UNMIPPED_0) * -4.0;
        vec4 a = texture2D(u_textures, v_texcoord_0, non_mipped_0);

        float cutout = bitValue(v_flags.x, FLAG_CUTOUT_0);
        if(cutout == 1.0 && a.a < 0.5) {
            discard;
        }
    #else // alpha
        vec4 a = texture2D(u_textures, v_texcoord_0);
    #endif

    a *= colorAndLightmap(v_color_0, 0, light);

    #if AO_SHADING_MODE != AO_MODE_NONE && CONTEXT_IS_BLOCK
        if(bitValue(v_flags.x, FLAG_DISABLE_AO_0) == 0.0) {
            a *= aoFactor;
        }
    #endif

    #if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
        if(bitValue(v_flags.x, FLAG_DISABLE_DIFFUSE_0) == 0.0) {
            a *= diffuse;
        }
    #endif

    #if LAYER_COUNT > 1
        float non_mipped_1 = bitValue(v_flags.y, FLAG_UNMIPPED_1) * -4.0;
        vec4 b = texture2D(u_textures, v_texcoord_1, non_mipped_1);
        float cutout_1 = bitValue(v_flags.y, FLAG_CUTOUT_1);
        if(cutout_1 != 1.0 || b.a >= 0.5) {
            b *= colorAndLightmap(v_color_1, 1, light);

            #if AO_SHADING_MODE != AO_MODE_NONE && CONTEXT_IS_BLOCK
                if(bitValue(v_flags.y, FLAG_DISABLE_AO_1) == 0.0) {
                    b *= aoFactor;
                }
            #endif

            #if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
                if(bitValue(v_flags.y, FLAG_DISABLE_DIFFUSE_1) == 0.0) {
                    b *= diffuse;
                }
            #endif
            a = vec4(mix(a.rgb, b.rgb, b.a), a.a);
        }
    #endif

    #if LAYER_COUNT > 2
        float non_mipped_2 = bitValue(v_flags.y, FLAG_UNMIPPED_2) * -4.0;
        vec4 c = texture2D(u_textures, v_texcoord_2, non_mipped_2);
        float cutout_2 = bitValue(v_flags.y, FLAG_CUTOUT_2);
        if(cutout_2 != 1.0 || c.a >= 0.5) {
            c *= colorAndLightmap(v_color_2, 2, light);

            #if AO_SHADING_MODE != AO_MODE_NONE && CONTEXT_IS_BLOCK
                if(bitValue(v_flags.y, FLAG_DISABLE_AO_2) == 0.0) {
                    c *= aoFactor;
                }
            #endif

            #if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
                if(bitValue(v_flags.y, FLAG_DISABLE_DIFFUSE_2) == 0.0) {
                    c *= diffuse;
                }
            #endif

            a = vec4(mix(a.rgb, c.rgb, c.a), a.a);
        }
    #endif

	return a;
}

/**
 * Linear fog.  Is an inverse factor - 0 means full fog.
 */
float linearFogFactor() {
	float fogFactor = (gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale;
	return clamp( fogFactor, 0.0, 1.0 );
}

/**
 * Exponential fog.  Is really an inverse factor - 0 means full fog.
 */
float expFogFactor() {
	float f = gl_FogFragCoord * gl_Fog.density;
    float fogFactor = u_fogMode == FOG_EXP ? exp(f) : exp(f * f);
    return clamp( 1.0 / fogFactor, 0.0, 1.0 );
}

/**
 * Returns either linear or exponential fog depending on current uniform value.
 */
float fogFactor() {
	return u_fogMode == FOG_LINEAR ? linearFogFactor() : expFogFactor();
}

vec4 fog(vec4 diffuseColor) {
#if CONTEXT == CONTEXT_ITEM_GUI
	return diffuseColor;
#elif SUBTLE_FOG
	float f = 1.0 - fogFactor();
	f *= f;
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, 1.0 - f);
#else
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, fogFactor());
#endif
}

