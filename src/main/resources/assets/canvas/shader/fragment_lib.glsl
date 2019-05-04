vec4 light;

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
//    #ifdef ENABLE_LIGHT_NOISE
//        vec4 n = texture2D(u_utility, v_noisecoord);
//        light = texture2D(u_lightmap, v_lightcoord + ((n.g - 0.5) * 0.015));
//	    vec4 smooth = texture2D(u_utility, v_hd_lightmap);
	    vec4 smooth = texture2D(u_utility, v_hd_lightmap / 512.0);
	    light = texture2D(u_lightmap, smooth.rg + 0.03125);
//	    light = texture2D(u_lightmap, v_lightcoord);
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
    	a = applyAo(a);
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
