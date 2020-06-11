#include canvas:shaders/lib/common_header.glsl
#include canvas:shaders/lib/common_varying.glsl
#include canvas:shaders/lib/diffuse.glsl
#include canvas:shaders/lib/flags.glsl
#include canvas:shaders/lib/fog.glsl
#include canvas:shaders/lib/fragment_data.glsl

#include canvas:apitarget

vec4 aoFactor(vec2 lightCoord) {
// Don't apply AO for item renders
#if CONTEXT_IS_BLOCK

	float ao = v_ao;

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

vec4 light() {
	#if CONTEXT_IS_GUI
		return vec4(1.0, 1.0, 1.0, 1.0);
	#else
		return texture2D(u_lightmap, v_lightcoord);
	#endif
}

vec4 diffuse() {
	#if DIFFUSE_SHADING_MODE == DIFFUSE_MODE_SKY_ONLY && CONTEXT_IS_BLOCK
		if (u_world[WORLD_HAS_SKYLIGHT] == 1.0 && u_world[WORLD_NIGHT_VISION] == 0) {
			float d = 1.0 - v_diffuse;
			d *= u_world[WORLD_EFFECTIVE_INTENSITY];
			d *= v_lightCoord.y;
			d += 0.03125;
			d = clamp(1.0 - d, 0.0, 1.0);
			return vec4(d, d, d, 1.0);
		} else {
			return vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
		}

	#elif DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
		return vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
	#endif
}

void main() {
	cv_FragmentInput fragInput = cv_FragmentInput (
		texture2D(u_textures, v_texcoord, cv_getFlag(FLAG_UNMIPPED) * -4.0),
		v_color,
		cv_getFlag(FLAG_EMISSIVE) == 1.0,
		cv_getFlag(FLAG_DISABLE_DIFFUSE) == 0.0
	);

	cv_startFragment(fragInput);

	cv_FragmentOutput fragOutput = cv_FragmentOutput (
		fragInput.spriteColor  * fragInput.vertexColor,
		fragInput.emissive
	);

	cv_endFragment(fragOutput);

    vec4 a = fragOutput.baseColor;

    if (a.a >= 0.5 || cv_getFlag(FLAG_CUTOUT) != 1.0) {
    	a *= fragOutput.emissive ? u_emissiveColor : light();

		#if AO_SHADING_MODE != AO_MODE_NONE && CONTEXT_IS_BLOCK
			if (cv_getFlag(FLAG_DISABLE_AO) == 0.0) {
				a *= aoFactor(v_lightcoord);
			}
		#endif

		#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
			if (fragInput.diffuse) {
				a *= diffuse();
			}
		#endif
    } else {
		discard;
	}

	gl_FragColor = fog(a);
}
