#include canvas:shaders/lib/common_header.glsl
#include canvas:shaders/lib/common_varying.glsl
#include canvas:shaders/lib/diffuse.glsl
#include canvas:shaders/lib/bitwise.glsl
#include canvas:shaders/lib/fog.glsl
#include canvas:shaders/vanilla/vanilla_varying.glsl
#include canvas:shaders/vanilla/vanilla_light.glsl

vec4 colorAndLightmap(vec4 fragmentColor, vec4 light) {
    return bitValue(v_flags, FLAG_EMISSIVE) == 0 ? light * fragmentColor : u_emissiveColor * fragmentColor;
}

vec4 diffuseColor() {

    #if !CONTEXT_IS_GUI
        vec2 lightCoord = lightCoord();
    #endif

    #if CONTEXT_IS_BLOCK
        vec4 light = texture2D(u_lightmap, lightCoord);
    #elif CONTEXT_IS_GUI
        vec4 light = vec4(1.0, 1.0, 1.0, 1.0);
    #else
        vec4 light = texture2D(u_lightmap, v_lightcoord);
    #endif

    #if HARDCORE_DARKNESS
        // TODO: encapsulate
        if (u_world[WORLD_HAS_SKYLIGHT] == 1.0 && u_world[WORLD_NIGHT_VISION] == 0.0) {
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

        if (u_world[WORLD_HAS_SKYLIGHT] == 1.0 && u_world[WORLD_NIGHT_VISION] == 0) {
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

    float non_mipped = bitValue(v_flags, FLAG_UNMIPPED) * -4.0;
    vec4 a = texture2D(u_textures, v_texcoord, non_mipped);

    if (a.a >= 0.5 || (bitValue(v_flags, FLAG_CUTOUT) != 1.0)) {
        a *= colorAndLightmap(v_color, light);

        #if AO_SHADING_MODE != AO_MODE_NONE && CONTEXT_IS_BLOCK
            if (bitValue(v_flags, FLAG_DISABLE_AO) == 0.0) {
                a *= aoFactor;
            }
        #endif

        #if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
            if (bitValue(v_flags, FLAG_DISABLE_DIFFUSE) == 0.0) {
                a *= diffuse;
            }
        #endif
    } else {
		discard;
	}

	return a;
}


