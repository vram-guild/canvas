
vec4 shadeColor(vec4 fragmentColor,  int layerIndex) {
	return bitValue(v_flags.x, layerIndex) == 0 ? v_light * fragmentColor : u_emissiveColor * fragmentColor;
}

vec4 diffuseColor()
{
#if CONTEXT == CONTEXT_BLOCK_SOLID
	float non_mipped = bitValue(v_flags.x, FLAG_UNMIPPED_0) * -4.0;
	vec4 a = texture2D(u_textures, v_texcoord_0, non_mipped);

	float cutout = bitValue(v_flags.x, FLAG_CUTOUT_0);
	if(cutout == 1.0 && a.a < 0.5)
		discard;
#else
		vec4 a = texture2D(u_textures, v_texcoord_0);
#endif

	vec4 shade = shadeColor(v_color_0, 0);

	a *= shade;

#if CONTEXT != CONTEXT_ITEM_GUI && CONTEXT != CONTEXT_ITEM_WORLD
    if(bitValue(v_flags.x, FLAG_DISABLE_AO_0) == 0.0) {
    	a *= vec4(v_ao, v_ao, v_ao, 1.0);
    }
#endif

    if(bitValue(v_flags.x, FLAG_DISABLE_DIFFUSE) == 0.0) {
    	a *= vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
    }

#if LAYER_COUNT > 1
    // TODO: honor non-mipped and cutout flags
	// TODO: honor shading flags
	vec4 b = texture2D(u_textures, v_texcoord_1) * shadeColor(v_color_1, 1);

	a = mix(a, b, b.a);
#endif

#if LAYER_COUNT > 2
	// TODO: honor non-mipped and cutout flags
	// TODO: honor shading flags
	vec4 c = texture2D(u_textures, v_texcoord_2) * shadeColor(v_color_2, 2);

	a = mix(a, c, c.a);
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
