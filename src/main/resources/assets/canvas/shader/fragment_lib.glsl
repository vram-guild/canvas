
vec4 shadeColor(vec4 fragmentColor,  int layerIndex)
{
	return bitValue(v_flags, layerIndex) == 0 ? v_light * fragmentColor : fragmentColor;
}

vec4 diffuseColor()
{
#ifdef SOLID
		float non_mipped = bitValue(v_flags, 3) * -4.0;
		vec4 a = texture2D(u_textures, v_texcoord_0, non_mipped);

		float cutout = bitValue(v_flags, 4);
		if(cutout == 1.0 && a.a < 0.5)
			discard;
#else
		vec4 a = texture2D(u_textures, v_texcoord_0);
#endif

	vec4 shade = shadeColor(v_color_0, 0);

	a *= shade;


#if LAYER_COUNT > 1
	vec4 b = texture2D(u_textures, v_texcoord_1) * shadeColor(v_color_1, 1);
	a = mix(a, b, b.a);
#endif

#if LAYER_COUNT > 2
	vec4 c = texture2D(u_textures, v_texcoord_2) * shadeColor(v_color_2, 2);
	a = mix(a, c, c.a);
#endif

	return a;
}

/**
 * Linear fog.  Is an inverse factor - 0 means full fog.
 */
float linearFogFactor()
{
	float fogFactor = (u_fogAttributes.x - gl_FogFragCoord)/u_fogAttributes.y;
	return clamp( fogFactor, 0.0, 1.0 );
}

/**
 * Exponential fog.  Is really an inverse factor - 0 means full fog.
 */
float expFogFactor()
{
    float fogFactor = 1.0 / exp(gl_FogFragCoord * u_fogAttributes.z);
    return clamp( fogFactor, 0.0, 1.0 );
}

/**
 * Returns either linear or exponential fog depending on current uniform value.
 */
float fogFactor()
{
	return u_fogAttributes.z == 0.0 ? linearFogFactor() : expFogFactor();
}

vec4 fog(vec4 diffuseColor)
{
	return mix(vec4(u_fogColor, diffuseColor.a), diffuseColor, fogFactor());
}
