vec4 light;

float flicker() {
    return u_world[WORLD_FLICKER];
}

float dimensionLight(float brightness) {
    float n = clamp(brightness * WORLD_DIM_LIGHT_LEN, 0.0, WORLD_DIM_LIGHT_LEN - 1);
    float f = fract(n);
    int low = int(floor(n)) + WORLD_DIM_LIGHT_0;
    int high = int(ceil(n)) + WORLD_DIM_LIGHT_0;
    return u_world[low] * (1.0 - f) + u_world[high] * f;

    return brightness / ((1.0 - brightness) * 3.0 + 1.0);
}

float worldAmbient() {
    return u_world[WORLD_AMBIENT];
}

vec3 outdoorLight(float brightness) {
    float w = worldAmbient();
    //TODO - include lightning effect
    float b = dimensionLight(brightness) * (w * 0.95 + 0.05);
    float rg = b * (w * 0.65 + 0.35);
    return vec3(rg, rg, b);
}

vec3 torchLight(float brightness) {
    float r = dimensionLight(brightness) * flicker();
    float g = r * ((r * 0.6 + 0.4) * 0.6 + 0.4);
    float b = r * (r * r * 0.6 + 0.4);
    return vec3(r, g, b);
}

float effectModifier() {
    return u_world[WORLD_EFFECT_MODIFIER];
}

vec3 applySkyDarkness(vec3 rgb) {
    float d = max(0.0, u_world[WORLD_SKY_DARKNESS]);

    if(d == 0.0) return rgb;

    vec3 sd = vec3(d, d, d);
    rgb = rgb * (vec3(1.0, 1.0, 1.0) - sd);
    rgb += rgb * sd * vec3(0.7, 0.6, 0.6);
    return rgb;
}

float gamma() {
    return u_world[WORLD_GAMMA];
}

vec3 gammaCorrect(vec3 rgb) {
    vec3 inv = vec3(1.0, 1.0, 1.0) - rgb;
    inv = vec3(1.0, 1.0, 1.0) - inv * inv * inv * inv;
    float gamma = gamma();
    vec3 result = rgb * (1.0 - gamma) + inv * gamma;
    result *= 0.96;
    result += 0.03;

    return clamp(result, 0.0, 1.0);
}

vec4 lightColor(vec2 brightness) {
    vec3 rgb = torchLight(brightness.x) + outdoorLight(brightness.y);
    rgb = rgb * 0.96 + 0.03;

    rgb = applySkyDarkness(rgb);

    //TODO: handle end override

    float effectModifier = effectModifier();
    //TODO: handle effects

    rgb = min(rgb, 1.0);

    return vec4(gammaCorrect(rgb), 1.0);
}

vec4 colorAndLightmap(vec4 fragmentColor,  int layerIndex) {
    return bitValue(v_flags.x, layerIndex) == 0 ? light * fragmentColor : u_emissiveColor * fragmentColor;
}

vec4 applyAo(vec4 baseColor) {
// Don't apply AO for item renders
#ifdef CONTEXT_IS_BLOCK
	return baseColor * vec4(v_ao, v_ao, v_ao, 1.0);
#else
	return baseColor;
#endif
}

//v_hd_blocklight
//v_hd_skylight
#ifdef ENABLE_SMOOTH_LIGHT
vec4 combinedLight(float offsetU, float offsetV) {
    vec2 offset = vec2(offsetU * LIGHTMAP_PIXEL_SIZE, offsetV * LIGHTMAP_PIXEL_SIZE);
    vec4 block = texture2D(u_utility, v_hd_blocklight + offset);
    vec4 sky = texture2D(u_utility, v_hd_blocklight + offset);
    vec2 lightCoord = vec2(block.r, sky.r);
    return texture2D(u_lightmap, lightCoord);
//    return vec4(1.0, 1.0, 1.0, 1.0);
}
#endif

vec4 diffuseColor() {

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

#ifdef CONTEXT_IS_BLOCK
	#ifdef ENABLE_SMOOTH_LIGHT
	    light = combinedLight(0.0, 0.0);
//	    light += combinedLight(-0.7, -0.7);
//	    light =  combinedLight(-1.0,  0.0);
//	    light += combinedLight(-0.7,  0.7);
//	    light += combinedLight( 0.0, -1.0);
//	    light += combinedLight( 0.0,  1.0);
//        light += combinedLight( 0.7, -0.7);
//        light += combinedLight( 1.0,  0.0);
//        light += combinedLight( 0.7,  0.7);
//        light /= 9.0;

        #ifdef ENABLE_LIGHT_NOISE
            vec4 dither = texture2D(u_dither, gl_FragCoord.xy / 8.0);
            lightCoord += dither.r / 64.0 - (1.0 / 128.0);
        #endif

    #else
        light = texture2D(u_lightmap, v_lightcoord);
    #endif

#elif CONTEXT == CONTEXT_ITEM_GUI
	light = vec4(1.0, 1.0, 1.0, 1.0);
#else
	light = texture2D(u_lightmap, v_lightcoord);
#endif

	a *= colorAndLightmap(v_color_0, 0);

    if(bitValue(v_flags.x, FLAG_DISABLE_AO_0) == 0.0) {
//    	a = applyAo(a);
    }

    if(bitValue(v_flags.x, FLAG_DISABLE_DIFFUSE_0) == 0.0) {
    	a *= vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
    }

#if LAYER_COUNT > 1
	float non_mipped_1 = bitValue(v_flags.y, FLAG_UNMIPPED_1) * -4.0;
	vec4 b = texture2D(u_textures, v_texcoord_1, non_mipped_1);
	float cutout_1 = bitValue(v_flags.y, FLAG_CUTOUT_1);
	if(cutout_1 != 1.0 || b.a >= 0.5) {
		b *= colorAndLightmap(v_color_1, 1);
		if(bitValue(v_flags.y, FLAG_DISABLE_AO_1) == 0.0) {
		    b = applyAo(b);
		}
		if(bitValue(v_flags.y, FLAG_DISABLE_DIFFUSE_1) == 0.0) {
			b *= vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
		}
		a = vec4(mix(a.rgb, b.rgb, b.a), a.a);
	}
#endif

#if LAYER_COUNT > 2
	float non_mipped_2 = bitValue(v_flags.y, FLAG_UNMIPPED_2) * -4.0;
	vec4 c = texture2D(u_textures, v_texcoord_2, non_mipped_2);
	float cutout_2 = bitValue(v_flags.y, FLAG_CUTOUT_2);
	if(cutout_2 != 1.0 || c.a >= 0.5) {
		c *= colorAndLightmap(v_color_2, 2);
		if(bitValue(v_flags.y, FLAG_DISABLE_AO_2) == 0.0) {
		    c = applyAo(c);
		}
		if(bitValue(v_flags.y, FLAG_DISABLE_DIFFUSE_2) == 0.0) {
			c *= vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
		}
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
#else
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, fogFactor());
#endif
}
