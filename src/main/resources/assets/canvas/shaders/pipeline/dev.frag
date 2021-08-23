#include canvas:shaders/pipeline/fog.glsl
#include canvas:shaders/pipeline/varying.glsl
#include canvas:shaders/pipeline/glint.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/api/view.glsl
#include frex:shaders/api/player.glsl
#include frex:shaders/api/material.glsl
#include frex:shaders/api/fragment.glsl
#include canvas:shaders/pipeline/glint.glsl
#include canvas:basic_light_config
#include canvas:handheld_light_config
#include canvas:shadow_debug
#include canvas:shaders/pipeline/shadow.glsl

/******************************************************
  canvas:shaders/pipeline/dev.frag
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

vec3 skyLight = frx_skyLightAtmosphericColor() * frx_skyLightColor() * (frx_skyLightTransitionFactor() * frx_skyLightIlluminance() / 32000.0);

vec3 shadowDist(int cascade) {
	vec4 c = frx_shadowCenter(cascade);

	return abs((c.xyz - shadowPos.xyz) / c.w);
}

int selectShadowCascade() {
	vec3 d3 = shadowDist(3);
	vec3 d2 = shadowDist(2);
	vec3 d1 = shadowDist(1);

	int cascade = 0;

	if (d3.x < 1.0 && d3.y < 1.0 && d3.z < 1.0) {
		cascade = 3;
	} else if (d2.x < 1.0 && d2.y < 1.0 && d2.z < 1.0) {
		cascade = 2;
	} else if (d1.x < 1.0 && d1.y < 1.0 && d1.z < 1.0) {
		cascade = 1;
	}

	return cascade;
}

#ifdef SHADOW_DEBUG

const vec4[] cascadeColors = vec4[4](
	vec4(1.0, 0.5, 0.5, 1.0),
	vec4(1.0, 1.0, 0.5, 1.0),
	vec4(0.5, 1.0, 0.5, 1.0),
	vec4(0.5, 1.0, 1.0, 1.0)
);

#endif

void frx_writePipelineFragment(in frx_FragmentData fragData) {
	vec4 a = fragData.spriteColor * fragData.vertexColor;

	if (frx_isGui() && !frx_isHand()) {
		if (fragData.diffuse) {
			float df = p_diffuseGui(frx_normal);
			df = df + (1.0 - df) * fragData.emissivity;
			a *= vec4(df, df, df, 1.0);
		}
	} else {

		// NB: this "lighting model" is a made-up garbage
		// temporary hack to see / test the shadow map quality
		// for testing - not a good way to do it

		// ambient
		float skyCoord = fragData.diffuse ? 0.03125 + (fragData.light.y - 0.03125) * 0.5 : fragData.light.y;
		vec4 light = frx_fromGamma(texture(frxs_lightmap, vec2(fragData.light.x, skyCoord)));
		light = mix(light, frx_emissiveColor(), fragData.emissivity);

	#if HANDHELD_LIGHT_RADIUS != 0
		vec4 held = frx_heldLight();

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

		light += shadow * vec4(skyLight * max(0.0, dot(frx_skyLightVector(), frx_normal)), 0.0);

	#ifdef SHADOW_DEBUG
		shadowCoords = abs(fract(shadowCoords * SHADOW_MAP_SIZE));

		if (!(shadowCoords.x > 0.05 && shadowCoords.x < 0.95 && shadowCoords.y > 0.05 && shadowCoords.y < 0.95)) {
			light = vec4(1.0);
			a = cascadeColors[cascade];
		}
	#endif

		if (fragData.ao) {
			light *= aoFactor(fragData.light, fragData.aoShade);
		}

		a *= frx_toGamma(light);
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

	if (frx_isGui()) {
		fragColor[TARGET_BASECOLOR] = a;
	} else {
		fragColor[TARGET_BASECOLOR] = p_fog(a);
		fragColor[TARGET_EMISSIVE] = vec4(fragData.emissivity * a.a, 0.0, 0.0, 1.0);
	}

	gl_FragDepth = gl_FragCoord.z;
}
