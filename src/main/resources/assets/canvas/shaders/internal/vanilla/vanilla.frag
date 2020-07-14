#include canvas:shaders/internal/header.glsl
#include canvas:shaders/internal/varying.glsl
#include canvas:shaders/internal/diffuse.glsl
#include canvas:shaders/internal/flags.glsl
#include canvas:shaders/internal/fog.glsl
#include canvas:shaders/api/world.glsl
#include canvas:shaders/api/player.glsl
#include canvas:shaders/api/material.glsl
#include canvas:shaders/api/fragment.glsl
#include canvas:shaders/api/sampler.glsl
#include canvas:shaders/lib/math.glsl
#include canvas:shaders/lib/color.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/vanilla/vanilla.frag
******************************************************/

vec4 aoFactor(vec2 lightCoord) {
	float ao = _cvv_ao;

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
            vec4 sky = texture2D(cvs_lightmap, vec2(0.03125, lightCoord.y));
            ao = mix(bao, ao, cv_luminance(sky.rgb));
            return vec4(ao, ao, ao, 1.0);
        #endif
    #else
        return vec4(ao, ao, ao, 1.0);
    #endif
}

vec4 light(cv_FragmentData fragData) {
	#if CONTEXT_IS_GUI
		return vec4(1.0, 1.0, 1.0, 1.0);
	#else

		#if DIFFUSE_SHADING_MODE == DIFFUSE_MODE_SKY_ONLY && CONTEXT_IS_BLOCK
			if (fragData.diffuse) {
				vec4 block = texture2D(cvs_lightmap, vec2(fragData.light.x, 0.03125));
				vec4 sky = texture2D(cvs_lightmap, vec2(0.03125, fragData.light.y));
				return max(block, sky * _cvv_diffuse);
			}
		#endif

		return texture2D(cvs_lightmap, fragData.light);
	#endif
}

void main() {
	cv_FragmentData fragData = cv_FragmentData (
		texture2D(cvs_spriteAltas, _cvv_texcoord, _cv_getFlag(_CV_FLAG_UNMIPPED) * -4.0),
		_cvv_color,
		cv_matEmissive() ? 1.0 : 0.0,
		!cv_matDisableDiffuse(),
		!cv_matDisableAo(),
		_cvv_normal,
		_cvv_lightcoord
	);

	cv_startFragment(fragData);

	vec4 raw = fragData.spriteColor * fragData.vertexColor;
    vec4 a = raw;

    if (a.a >= 0.5 || _cv_getFlag(_CV_FLAG_CUTOUT) != 1.0) {
    	a *= mix(light(fragData), cv_emissiveColor(), fragData.emissivity);

		#if AO_SHADING_MODE != AO_MODE_NONE && CONTEXT_IS_BLOCK
			if (fragData.ao) {
				a *= aoFactor(fragData.light);
			}
		#endif

		#if DIFFUSE_SHADING_MODE == DIFFUSE_MODE_NORMAL
			if (fragData.diffuse) {
				float df = _cvv_diffuse + (1.0 - _cvv_diffuse) * fragData.emissivity;

				a *= vec4(df, df, df, 1.0);
			}
		#endif
    } else {
		discard;
	}

    // TODO: need a separate fog pass?
    gl_FragData[TARGET_BASECOLOR] = _cv_fog(a);

	#if TARGET_EMISSIVE > 0
		gl_FragData[TARGET_EMISSIVE] = raw * fragData.emissivity;
	#endif
}
