/******************************************************
  frex:shaders/lib/noise/noisecommon.glsl

  External MIT noise library - bundled for convenience.

  No modifications have been made except to remove
  the #version header, add this comment block, and
  move some shared functions to this file.
******************************************************/

// Modulo 289 without a division (only multiplications)
vec4 mod289(vec4 x) {
	return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec3 mod289(vec3 x) {
	return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec2 mod289(vec2 x) {
	return x - floor(x * (1.0 / 289.0)) * 289.0;
}

float mod289(float x) {
	return x - floor(x * (1.0 / 289.0)) * 289.0;
}

// Modulo 7 without a division
vec4 mod7(vec4 x) {
	return x - floor(x * (1.0 / 7.0)) * 7.0;
}

vec3 mod7(vec3 x) {
	return x - floor(x * (1.0 / 7.0)) * 7.0;
}

// Permutation polynomial: (34x^2 + x) mod 289
// ring size 289 = 17*17
vec4 permute(vec4 x) {
	return mod289((34.0 * x + 1.0) * x);
}

vec3 permute(vec3 x) {
	return mod289((34.0 * x + 1.0) * x);
}

float permute(float x) {
	return mod289(((x*34.0)+1.0)*x);
}

vec4 taylorInvSqrt(vec4 r) {
	return 1.79284291400159 - 0.85373472095314 * r;
}

float taylorInvSqrt(float r) {
	return 1.79284291400159 - 0.85373472095314 * r;
}
