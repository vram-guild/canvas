/******************************************************
  frex:shaders/lib/color.glsl

  Common color processing functions.

  Portions taken from Wisdom Shaders by Cheng (Bob) Cao, Apache 2.0 license
  https://github.com/bobcao3/Wisdom-Shaders

******************************************************/

const float FRX_GAMMA = 2.4;
const float FRX_INVERSE_GAMMA = 1.0 / FRX_GAMMA;

vec4 frx_fromGamma(vec4 c) {
	return pow(c, vec4(FRX_GAMMA));
}

vec4 frx_toGamma(vec4 c) {
	return pow(c, vec4(FRX_INVERSE_GAMMA));
}

vec3 frx_fromGamma(vec3 c) {
	return pow(c, vec3(FRX_GAMMA));
}

vec3 frx_toGamma(vec3 c) {
	return pow(c, vec3(FRX_INVERSE_GAMMA));
}

//float frx_luma(vec3 c) {
//    return dot(c, vec3(0.2126, 0.7152, 0.0722));
//}

const mat3 FRX_ACES_INPUT_MATRIX = mat3(
vec3(0.59719, 0.07600, 0.02840),
vec3(0.35458, 0.90834, 0.13383),
vec3(0.04823, 0.01566, 0.83777)
);

// ODT_SAT => XYZ => D60_2_D65 => sRGB
const mat3 FRX_ACES_OUTPUT_MATRIX = mat3(
vec3(1.60475, -0.10208, -0.00327),
vec3(-0.53108, 1.10813, -0.07276),
vec3(-0.07367, -0.00605, 1.07602)
);

vec3 FRX_RRT_AND_ODTF_FIT(vec3 v) {
	vec3 a = v * (v + 0.0245786f) - 0.000090537f;
	vec3 b = v * (0.983729f * v + 0.4329510f) + 0.238081f;
	return a / b;
}

vec3 frx_toneMap(vec3 color) {
	color = FRX_ACES_INPUT_MATRIX * color;
	color = FRX_RRT_AND_ODTF_FIT(color);
	return FRX_ACES_OUTPUT_MATRIX * color;
}
