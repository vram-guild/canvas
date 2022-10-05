#include abstract:shaders/pipeline/fog.glsl
#include abstract:shaders/pipeline/varying.glsl
#include abstract:shaders/pipeline/glint.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/api/view.glsl
#include frex:shaders/api/player.glsl
#include frex:shaders/api/material.glsl
#include frex:shaders/api/fragment.glsl
#include abstract:shaders/pipeline/glint.glsl
#include abstract:basic_light_config
#include abstract:handheld_light_config
#include abstract:shadow_debug
#include abstract:shaders/pipeline/shadow.glsl

/******************************************************
  abstract:shaders/pipeline/dev.frag
******************************************************/

#define TARGET_BASECOLOR 0
#define TARGET_EMISSIVE  1

in vec4 shadowPos;
out vec4[2] fragColor;

#if HANDHELD_LIGHT_RADIUS != 0
flat in float _cvInnerAngle;
flat in float _cvOuterAngle;
in vec4 _cvViewVertex;
#endif

/**
 * Offers results similar to vanilla in GUI, assumes a fixed transform.
 */
float p_diffuseGui(vec3 normal) {
	normal = normalize(normal);
	float light = 0.4
		+ 0.6 * clamp(dot(normal.xyz, vec3(-0.93205774, 0.26230583, -0.24393857)), 0.0, 1.0)
		+ 0.6 * clamp(dot(normal.xyz, vec3(-0.10341814, 0.9751613, 0.18816751)), 0.0, 1.0);
	return min(light, 1.0);
}

vec4 aoFactor(vec2 lightCoord, float ao) {
	// accelerate the transition from 0.4 (should be the minimum) to 1.0
	float bao = (ao - 0.4) / 0.6;
	bao = clamp(bao, 0.0, 1.0);
	bao = 1.0 - bao;
	bao = bao * bao * (1.0 - lightCoord.x * 0.6);
	bao = 0.4 + (1.0 - bao) * 0.6;

	vec4 sky = texture(frxs_lightmap, vec2(0.03125, lightCoord.y));
	ao = mix(bao, ao, frx_luminance(sky.rgb));
	return vec4(ao, ao, ao, 1.0);
}

vec3 skyLight = frx_skyLightAtmosphericColor * frx_skyLightColor * (frx_skyLightTransitionFactor * frx_skyLightIlluminance / 32000.0);

vec3 shadowDist(int cascade) {
	vec4 c = frx_shadowCenter(cascade);
	return abs((c.xyz - shadowPos.xyz) / c.w);
}

int selectShadowCascade() {
	if (all(lessThan(shadowDist(3), vec3(1.0)))) return 3;
	if (all(lessThan(shadowDist(2), vec3(1.0)))) return 2;
	if (all(lessThan(shadowDist(1), vec3(1.0)))) return 1;

	return 0;
}

#ifdef SHADOW_DEBUG

const vec4[] cascadeColors = vec4[4](
	vec4(1.0, 0.5, 0.5, 1.0),
	vec4(1.0, 1.0, 0.5, 1.0),
	vec4(0.5, 1.0, 0.5, 1.0),
	vec4(0.5, 1.0, 1.0, 1.0)
);

#endif

void frx_pipelineFragment() {
	vec4 a = frx_fragColor;

	if (frx_isGui && !frx_isHand) {
		if (frx_fragEnableDiffuse) {
			float df = p_diffuseGui(frx_vertexNormal);
			df = df + (1.0 - df) * frx_fragEmissive;
			a *= vec4(df, df, df, 1.0);
		}
	} else {

		// NB: this "lighting model" is a made-up garbage
		// temporary hack to see / test the shadow map quality
		// for testing - not a good way to do it

		// ambient
		float skyCoord = frx_fragEnableDiffuse ? 0.03125 + (frx_fragLight.y - 0.03125) * 0.5 : frx_fragLight.y;
		vec4 light = frx_fromGamma(texture(frxs_lightmap, vec2(frx_fragLight.x, skyCoord)));
		light = mix(light, frx_emissiveColor, frx_fragEmissive);

	#if HANDHELD_LIGHT_RADIUS != 0
		vec4 held = frx_heldLight;

		if (held.w > 0.0) {
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

			held = vec4(held.xyz, 0.0) * maxBlock * d;

			light += held;
		}
	#endif
		int cascade = selectShadowCascade();

		// NB: perspective division should not be needed because ortho projection
		vec4 shadowCoords = frx_shadowProjectionMatrix(cascade) * shadowPos;

		// Transform from screen coordinates to texture coordinates
		vec3 shadowTexCoords = shadowCoords.xyz * 0.5 + 0.5;
		float shadow = sampleShadowPCF(shadowTexCoords, float(cascade));

		// WIP: contact shadows
		//float gradientNoise = TAU * InterleavedGradientNoise(gl_FragCoord.xy);
		//float spread = 3.0; //Penumbra(gradientNoise, shadowTexCoords, float(cascade));
		//float shadow = sampleVogelShadowPCF(shadowTexCoords, float(cascade), gradientNoise, spread);

		light += shadow * vec4(skyLight * max(0.0, dot(frx_skyLightVector, frx_vertexNormal)), 0.0);

	#ifdef SHADOW_DEBUG
		shadowCoords = abs(fract(shadowCoords * SHADOW_MAP_SIZE));

		if (!(shadowCoords.x > 0.05 && shadowCoords.x < 0.95 && shadowCoords.y > 0.05 && shadowCoords.y < 0.95)) {
			light = vec4(1.0);
			a = cascadeColors[cascade];
		}
	#endif

		if (frx_fragEnableAo) {
			light *= aoFactor(frx_fragLight.xy, frx_vertexLight.z);
		}

		a *= frx_toGamma(light);
	}

	if (frx_matFlash == 1) {
		a = a * 0.25 + 0.75;
	} else if (frx_matHurt == 1) {
		a = vec4(0.25 + a.r * 0.75, a.g * 0.75, a.b * 0.75, a.a);
	}

	glintify(a, float(frx_matGlint));

	if (frx_isGui) {
		fragColor[TARGET_BASECOLOR] = a;
	} else {
		fragColor[TARGET_BASECOLOR] = p_fog(a);
		fragColor[TARGET_EMISSIVE] = vec4(frx_fragEmissive * a.a, 0.0, 0.0, 1.0);
	}

	gl_FragDepth = gl_FragCoord.z;
}
