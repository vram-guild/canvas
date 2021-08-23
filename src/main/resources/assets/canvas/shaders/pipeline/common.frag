#include canvas:shaders/pipeline/fog.glsl
#include canvas:shaders/pipeline/diffuse.glsl
#include canvas:shaders/pipeline/glint.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/api/view.glsl
#include frex:shaders/api/player.glsl
#include frex:shaders/api/material.glsl
#include frex:shaders/api/fragment.glsl
#include canvas:basic_light_config
#include canvas:handheld_light_config

/******************************************************
  canvas:shaders/pipeline/common.frag
******************************************************/

#define TARGET_BASECOLOR 0
#define TARGET_EMISSIVE  1

#ifdef SHADOW_MAP_PRESENT
in vec4 shadowPos;
#endif

#if HANDHELD_LIGHT_RADIUS != 0
flat in float _cvInnerAngle;
flat in float _cvOuterAngle;
in vec4 _cvViewVertex;
#endif

#if AO_SHADING_MODE != AO_MODE_NONE
vec4 aoFactor(vec2 lightCoord, float ao) {

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
	vec4 sky = texture(frxs_lightmap, vec2(0.03125, lightCoord.y));
	ao = mix(bao, ao, frx_luminance(sky.rgb));
	return vec4(ao, ao, ao, 1.0);
	#endif
#else
	return vec4(ao, ao, ao, 1.0);
#endif
}
#endif

vec4 light(frx_FragmentData fragData) {
	vec4 result;

#if DIFFUSE_SHADING_MODE == DIFFUSE_MODE_SKY_ONLY
	if (fragData.diffuse) {
		vec4 block = texture(frxs_lightmap, vec2(fragData.light.x, 0.03125));
		vec4 sky = texture(frxs_lightmap, vec2(0.03125, fragData.light.y));
		result = max(block, sky * pv_diffuse);
	} else {
		result = texture(frxs_lightmap, fragData.light);
	}
#else
	result = texture(frxs_lightmap, fragData.light);
#endif

#if HANDHELD_LIGHT_RADIUS != 0
	vec4 held = frx_heldLight();

	if (held.w > 0.0 && (!frx_isGui() || frx_isHand())) {
		float d = clamp(frx_distance / (held.w * HANDHELD_LIGHT_RADIUS), 0.0, 1.0);
		d = 1.0 - d * d;

		// handle spot lights
		if (_cvInnerAngle != 0.0) {
			float distSq = _cvViewVertex.x * _cvViewVertex.x + _cvViewVertex.y * _cvViewVertex.y;
			float innerLimitSq = _cvInnerAngle * frx_distance;
			innerLimitSq *= innerLimitSq;
			float outerLimitSq = _cvOuterAngle * frx_distance;
			outerLimitSq *= outerLimitSq;

			d = distSq < innerLimitSq ? d :
					distSq < outerLimitSq ? d * (1.0 - (distSq - innerLimitSq) / (outerLimitSq - innerLimitSq)) : 0.0;
		}

		vec4 maxBlock = texture(frxs_lightmap, vec2(0.96875, 0.03125));

		held = vec4(held.xyz, 1.0) * maxBlock * d;

		result = min(result + held, 1.0);
	}
#endif

	return result;
}

vec4 p_writeBaseColorAndDepth(in frx_FragmentData fragData) {
	vec4 a = fragData.spriteColor * fragData.vertexColor;

	if (frx_isGui() && !frx_isHand()) {
		if (fragData.diffuse) {
			float df = p_diffuseGui(frx_normal);
			df = df + (1.0 - df) * fragData.emissivity;
			a *= vec4(df, df, df, 1.0);
		}
	} else {
		a *= mix(light(fragData), frx_emissiveColor(), fragData.emissivity);

	#if AO_SHADING_MODE != AO_MODE_NONE
		a *= frx_fragEnableAo ? aoFactor(fragData.light, frx_fragAo * frx_fragLight.z) : 1.0;
	#endif

	#if DIFFUSE_SHADING_MODE == DIFFUSE_MODE_NORMAL
		if (frx_fragEnableDiffuse) {
			float df = pv_diffuse + (1.0 - pv_diffuse) * fragData.emissivity;

			a *= vec4(df, df, df, 1.0);
		}
	#endif
	}

	if (frx_matFlash()) {
		a = a * 0.25 + 0.75;
	} else if (frx_matHurt()) {
		a = vec4(0.25 + a.r * 0.75, a.g * 0.75, a.b * 0.75, a.a);
	}

	glintify(a, frx_matGlint());

	// WIP: remove - various api tests
	//a = min(vec4(1.0, 1.0, 1.0, 1.0), a + vec4(frx_smoothedEyeBrightness().y));
	//a = min(vec4(1.0, 1.0, 1.0, 1.0), a + vec4(0.0, 0.0, frx_eyePos().y / 255.0, 0.0));
	//if (frx_playerFlag(FRX_PLAYER_EYE_IN_LAVA)) {
	//	a = min(vec4(1.0, 1.0, 1.0, 1.0), a + vec4(0.0, 0.0, 1.0, 0.0));
	//}
	//a = min(vec4(1.0, 1.0, 1.0, 1.0), a + vec4(frx_rainGradient()));
	//a = min(vec4(1.0, 1.0, 1.0, 1.0), a + vec4(frx_smoothedRainGradient()));
	//if (frx_renderTarget() == TARGET_ENTITY) {
	//	a = vec4(0.0, 1.0, 0.0, a.a);
	//}
	//a = vec4(frx_vanillaClearColor(), a.a);

	fragColor[TARGET_BASECOLOR] = p_fog(a);
	gl_FragDepth = gl_FragCoord.z;

	return a;
}
