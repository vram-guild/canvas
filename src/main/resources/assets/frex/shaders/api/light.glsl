/****************************************************************
 * frex:shaders/api/light.glsl - Canvas Implementation
 ***************************************************************/

#ifdef COLORED_LIGHTS_ENABLED

#ifdef SPARSE_LIGHT_DATA
#define LightSampler sampler2D

bool _cv_hasLightData(vec3 worldPos) {
	return false;
}

vec2 _cv_lightCoords(vec3 worldPos) {
	return vec2(0.0);
}

ivec2 _cv_lightTexelCoords(vec3 worldPos) {
	return ivec2(0);
}
#else
#define LightSampler sampler3D

bool _cv_hasLightData(vec3 worldPos) {
	return clamp(worldPos, _cvu_world[_CV_LIGHT_DATA_ORIGIN].xyz, _cvu_world[_CV_LIGHT_DATA_ORIGIN].xyz + _CV_LIGHT_DATA_SIZE) == worldPos;
}

vec3 _cv_lightCoords(vec3 worldPos) {
	return mod(worldPos, _CV_LIGHT_DATA_SIZE) / _CV_LIGHT_DATA_SIZE;
}

ivec3 _cv_lightTexelCoords(vec3 worldPos) {
	return ivec3(mod(clamp(worldPos, _cvu_world[_CV_LIGHT_DATA_ORIGIN].xyz, _cvu_world[_CV_LIGHT_DATA_ORIGIN].xyz + _CV_LIGHT_DATA_SIZE), _CV_LIGHT_DATA_SIZE));
}
#endif

bool _cv_isUseful(float a) {
	return (int(a * 15.0) & 8) > 0;
}

bool _cv_isOccluder(float a) {
	return (int(a * 15.0) & 2) > 0;
}

#if !defined(SPARSE_LIGHT_DATA) || defined(_CV_LIGHT_DATA_COMPLEX_FILTER)
vec3 _cv_getLightFiltered(LightSampler lightSampler, vec3 worldPos, vec3 fallback) {
	if (!_cv_hasLightData(worldPos)) {
		return fallback;
	}

	vec3 pos = worldPos - vec3(0.5);

	#ifdef _CV_LIGHT_DATA_COMPLEX_FILTER
	pos = floor(pos) + vec3(0.5);
	#endif

	vec4 tex000 = texelFetch(lightSampler, _cv_lightTexelCoords(pos + vec3(0.0, 0.0, 0.0)), 0);
	vec4 tex001 = texelFetch(lightSampler, _cv_lightTexelCoords(pos + vec3(0.0, 0.0, 1.0)), 0);
	vec4 tex010 = texelFetch(lightSampler, _cv_lightTexelCoords(pos + vec3(0.0, 1.0, 0.0)), 0);
	vec4 tex011 = texelFetch(lightSampler, _cv_lightTexelCoords(pos + vec3(0.0, 1.0, 1.0)), 0);
	vec4 tex101 = texelFetch(lightSampler, _cv_lightTexelCoords(pos + vec3(1.0, 0.0, 1.0)), 0);
	vec4 tex110 = texelFetch(lightSampler, _cv_lightTexelCoords(pos + vec3(1.0, 1.0, 0.0)), 0);
	vec4 tex100 = texelFetch(lightSampler, _cv_lightTexelCoords(pos + vec3(1.0, 0.0, 0.0)), 0);
	vec4 tex111 = texelFetch(lightSampler, _cv_lightTexelCoords(pos + vec3(1.0, 1.0, 1.0)), 0);

	#ifdef _CV_LIGHT_DATA_COMPLEX_FILTER
	vec3 center = worldPos - pos;
	vec3 pos000 = vec3(0.0, 0.0, 0.0) - center;
	vec3 pos001 = vec3(0.0, 0.0, 1.0) - center;
	vec3 pos010 = vec3(0.0, 1.0, 0.0) - center;
	vec3 pos011 = vec3(0.0, 1.0, 1.0) - center;
	vec3 pos101 = vec3(1.0, 0.0, 1.0) - center;
	vec3 pos110 = vec3(1.0, 1.0, 0.0) - center;
	vec3 pos100 = vec3(1.0, 0.0, 0.0) - center;
	vec3 pos111 = vec3(1.0, 1.0, 1.0) - center;

	float w000 = float(_cv_isUseful(tex000.a)) * abs(pos111.x * pos111.y * pos111.z);
	float w001 = float(_cv_isUseful(tex001.a)) * abs(pos110.x * pos110.y * pos110.z);
	float w010 = float(_cv_isUseful(tex010.a)) * abs(pos101.x * pos101.y * pos101.z);
	float w011 = float(_cv_isUseful(tex011.a)) * abs(pos100.x * pos100.y * pos100.z);
	float w101 = float(_cv_isUseful(tex101.a)) * abs(pos010.x * pos010.y * pos010.z);
	float w110 = float(_cv_isUseful(tex110.a)) * abs(pos001.x * pos001.y * pos001.z);
	float w100 = float(_cv_isUseful(tex100.a)) * abs(pos011.x * pos011.y * pos011.z);
	float w111 = float(_cv_isUseful(tex111.a)) * abs(pos000.x * pos000.y * pos000.z);

	float weight = w000 + w001 + w010 + w011 + w101 + w110 + w100 + w111;
	vec3 finalMix = weight == 0.0 ? vec3(0.0) : (tex000.rgb * w000 + tex001.rgb * w001 + tex010.rgb * w010 + tex011.rgb * w011 + tex101.rgb * w101 + tex110.rgb * w110 + tex100.rgb * w100 + tex111.rgb * w111) / weight;
	#else
	vec3 fac = fract(pos);

	vec3 mix001 = mix(tex000.rgb, tex001.rgb, fac.z);
	vec3 mix011 = mix(tex010.rgb, tex011.rgb, fac.z);
	vec3 mix010 = mix(mix001, mix011, fac.y);

	vec3 mix101 = mix(tex100.rgb, tex101.rgb, fac.z);
	vec3 mix111 = mix(tex110.rgb, tex111.rgb, fac.z);
	vec3 mix110 = mix(mix101, mix111, fac.y);

	vec3 finalMix = mix(mix010, mix110, fac.x);
	#endif

	return finalMix;

//	HALL OF SHAME
//
//	#ifdef _CV_LIGHT_DATA_COMPLEX_FILTER
//	vec3 cTex = texelFetch(lightSampler, _cv_lightTexelCoords(worldPos), 0).rgb;
//	tex000 = tex000 == vec3(0.0) ? cTex : tex000;
//	tex001 = tex001 == vec3(0.0) ? cTex : tex001;
//	tex010 = tex010 == vec3(0.0) ? cTex : tex010;
//	tex011 = tex011 == vec3(0.0) ? cTex : tex011;
//	tex100 = tex100 == vec3(0.0) ? cTex : tex100;
//	tex101 = tex101 == vec3(0.0) ? cTex : tex101;
//	tex110 = tex110 == vec3(0.0) ? cTex : tex110;
//	tex111 = tex111 == vec3(0.0) ? cTex : tex111;
//	#endif
//
//	float w000 = tex000 == vec3(0.0) ? 0.0 : 1.0 - (dot(abs((vec3(0.0, 0.0, 0.0) - center)), vec3(1.0)) / 3.0);
//	float w001 = tex001 == vec3(0.0) ? 0.0 : 1.0 - (dot(abs((vec3(0.0, 0.0, 1.0) - center)), vec3(1.0)) / 3.0);
//	float w010 = tex010 == vec3(0.0) ? 0.0 : 1.0 - (dot(abs((vec3(0.0, 1.0, 0.0) - center)), vec3(1.0)) / 3.0);
//	float w011 = tex011 == vec3(0.0) ? 0.0 : 1.0 - (dot(abs((vec3(0.0, 1.0, 1.0) - center)), vec3(1.0)) / 3.0);
//
//	float w101 = tex101 == vec3(0.0) ? 0.0 : 1.0 - (dot(abs((vec3(1.0, 0.0, 1.0) - center)), vec3(1.0)) / 3.0);
//	float w110 = tex110 == vec3(0.0) ? 0.0 : 1.0 - (dot(abs((vec3(1.0, 1.0, 0.0) - center)), vec3(1.0)) / 3.0);
//	float w100 = tex100 == vec3(0.0) ? 0.0 : 1.0 - (dot(abs((vec3(1.0, 0.0, 0.0) - center)), vec3(1.0)) / 3.0);
//	float w111 = tex111 == vec3(0.0) ? 0.0 : 1.0 - (dot(abs((vec3(1.0, 1.0, 1.0) - center)), vec3(1.0)) / 3.0);

//	vec3 mix001 = tex000 == vec3(0.0) ? tex001 : (tex001 == vec3(0.0) ? tex000 : mix(tex000, tex001, fac.z));
//	vec3 mix011 = tex010 == vec3(0.0) ? tex011 : (tex011 == vec3(0.0) ? tex010 : mix(tex010, tex011, fac.z));
//	vec3 mix010 = mix001 == vec3(0.0) ? mix011 : (mix011 == vec3(0.0) ? mix001 : mix(mix001, mix011, fac.y));
//
//	vec3 mix101 = tex100 == vec3(0.0) ? tex101 : (tex101 == vec3(0.0) ? tex100 : mix(tex100, tex101, fac.z));
//	vec3 mix111 = tex110 == vec3(0.0) ? tex111 : (tex111 == vec3(0.0) ? tex110 : mix(tex110, tex111, fac.z));
//	vec3 mix110 = mix101 == vec3(0.0) ? mix111 : (mix111 == vec3(0.0) ? mix101 : mix(mix101, mix111, fac.y));
//
//	vec3 finalMix = mix010 == vec3(0.0) ? mix110 : (mix110 == vec3(0.0) ? mix010 : mix(mix010, mix110, fac.x));

//	float w0 = 1.0 - (fac.x + fac.y + fac.z) * 0.33333333333;
//
//	float w1 = fac.x;
//	float w2 = fac.y;
//	float w3 = fac.z;
//	float w4 = (fac.x + fac.y) * 0.5;
//	float w5 = (fac.y + fac.z) * 0.5;
//	float w6 = (fac.x + fac.z) * 0.5;
//	float w7 = (fac.x + fac.y + fac.z) * 0.33333333333;
//
//	#ifdef _CV_LIGHT_DATA_COMPLEX_FILTER
//	w1 *= (tex1 == vec3(0.0)) ? 0.0 : 1.0;
//	w2 *= (tex2 == vec3(0.0)) ? 0.0 : 1.0;
//	w3 *= (tex3 == vec3(0.0)) ? 0.0 : 1.0;
//	w4 *= (tex4 == vec3(0.0)) ? 0.0 : 1.0;
//	w5 *= (tex5 == vec3(0.0)) ? 0.0 : 1.0;
//	w6 *= (tex6 == vec3(0.0)) ? 0.0 : 1.0;
//	w7 *= (tex7 == vec3(0.0)) ? 0.0 : 1.0;
//	#endif

//	float weight = w0 + w1 + w2 + w3 + w4 + w5 + w6 + w7;
//
//	return (tex0 * w0 + tex1 * w1 + tex2 * w2 + tex3 * w3 + tex4 * w4 + tex5 * w5 + tex6 * w6 + tex7 * w7) / weight;
}
#endif

vec4 frx_getLightRaw(LightSampler lightSampler, vec3 worldPos) {
	if (!_cv_hasLightData(worldPos)) {
		return vec4(0.0);
	}

	vec4 tex = texture(lightSampler, _cv_lightCoords(worldPos));
	return vec4(tex.rgb, float(_cv_isUseful(tex.a)));
}

bool frx_lightDataExists(vec3 worldPos) {
	return _cv_hasLightData(worldPos);
}

#ifdef _CV_LIGHT_DATA_COMPLEX_FILTER
	#define frx_getLightFiltered _cv_getLightFiltered
#else
	#ifdef SPARSE_LIGHT_DATA
		#define frx_getLightFiltered _cv_getLightFiltered
	#else
		#define frx_getLightFiltered frx_getLightRaw
	#endif
#endif

#endif
