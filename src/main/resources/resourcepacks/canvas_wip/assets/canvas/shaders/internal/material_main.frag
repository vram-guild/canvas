#include canvas:shaders/internal/header.glsl
#include canvas:shaders/internal/varying.glsl
#include canvas:shaders/internal/diffuse.glsl
#include canvas:shaders/internal/flags.glsl
#include canvas:shaders/internal/fog.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/api/player.glsl
#include frex:shaders/api/material.glsl
#include frex:shaders/api/fragment.glsl
#include frex:shaders/api/sampler.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/lib/color.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/material_main.frag
******************************************************/

void _cv_startFragment(inout frx_FragmentData data) {
	int cv_programId = _cv_fragmentProgramId();

#include canvas:startfragment
}

#if AO_SHADING_MODE != AO_MODE_NONE
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
	vec4 sky = texture2D(frxs_lightmap, vec2(0.03125, lightCoord.y));
	ao = mix(bao, ao, frx_luminance(sky.rgb));
	return vec4(ao, ao, ao, 1.0);
	#endif
#else
	return vec4(ao, ao, ao, 1.0);
#endif
}
#endif

vec4 lightAt(vec3 pos) {
	pos = mod(pos, 16.0);
	vec3 dist = (pos - vec3(8.0, 8.0, 8.0)) / 12.0;
	dist *= dist;
	float d = max(0.0, 1.0 - dist.x - dist.y - dist.z);
	return vec4(d, d, d, 1.0);
}

vec4 lightSample(vec3 pos, vec3 normal) {
	return lightAt(pos + normal * 0.05);
}

vec4 light(frx_FragmentData fragData) {
	vec4 result;
	vec4 block = lightSample(_cvv_worldcoord, _cvv_normal);
	vec4 sky = texture2D(frxs_lightmap, vec2(0.03125, fragData.light.y));

#if DIFFUSE_SHADING_MODE == DIFFUSE_MODE_SKY_ONLY
	if (fragData.diffuse) {
		result = min(vec4(1.0, 1.0, 1.0, 1.0), block + sky * _cvv_diffuse);
	} else {
		result = min(vec4(1.0, 1.0, 1.0, 1.0), block + sky);
	}
#else
	result = result = min(vec4(1.0, 1.0, 1.0, 1.0), block + sky);
#endif

#if HANDHELD_LIGHT_RADIUS != 0
	vec4 held = frx_heldLight();

	if (held.w > 0.0 && !frx_isGui()) {
		float d = clamp(gl_FogFragCoord / (held.w * HANDHELD_LIGHT_RADIUS), 0.0, 1.0);
		d = 1.0 - d * d;

		vec4 maxBlock = texture2D(frxs_lightmap, vec2(0.96875, 0.03125));

		held = vec4(held.xyz, 1.0) * maxBlock * d;

		result = min(result + held, 1.0);
	}
#endif

	return result;
}

void main() {
#ifndef PROGRAM_BY_UNIFORM
	if (_cv_programDiscard()) {
		discard;
	}
#endif

	frx_FragmentData fragData = frx_FragmentData (
		texture2D(frxs_spriteAltas, _cvv_texcoord, _cv_getFlag(_CV_FLAG_UNMIPPED) * -4.0),
		_cvv_color,
		frx_matEmissive() ? 1.0 : 0.0,
		!frx_matDisableDiffuse(),
		!frx_matDisableAo(),
		_cvv_normal,
		_cvv_lightcoord
	);

	_cv_startFragment(fragData);

	vec4 raw = fragData.spriteColor * fragData.vertexColor;
	vec4 a = raw;

	// PERF: varyings better here?
	if (_cv_getFlag(_CV_FLAG_CUTOUT) == 1.0) {
		float t = _cv_getFlag(_CV_FLAG_TRANSLUCENT_CUTOUT) == 1.0 ? _CV_TRANSLUCENT_CUTOUT_THRESHOLD : 0.5;

		if (a.a < t) {
			discard;
		}
	}

	a *= mix(light(fragData), frx_emissiveColor(), fragData.emissivity);

#if AO_SHADING_MODE != AO_MODE_NONE
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

	// PERF: varyings better here?
	if (_cv_getFlag(_CV_FLAG_FLASH_OVERLAY) == 1.0) {
		a = a * 0.25 + 0.75;
	} else if (_cv_getFlag(_CV_FLAG_HURT_OVERLAY) == 1.0) {
		a = vec4(0.25 + a.r * 0.75, a.g * 0.75, a.b * 0.75, a.a);
	}

	// TODO: need a separate fog pass?
	gl_FragData[TARGET_BASECOLOR] = _cv_fog(a);
	gl_FragDepth = gl_FragCoord.z;

#if TARGET_EMISSIVE > 0
	gl_FragData[TARGET_EMISSIVE] = vec4(fragData.emissivity * a.a, 0.0, 0.0, 1.0);
#endif
}
