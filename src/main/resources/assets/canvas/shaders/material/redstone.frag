#include canvas:shaders/lib/fragment_data.glsl
#include canvas:shaders/lib/math.glsl

void cv_startFragment(inout cv_FragmentInput inputData) {
	bool lit = (inputData.spriteColor.r - inputData.spriteColor.g) > 0.15f || luminance(inputData.spriteColor.rgb) > 0.9;
	inputData.emissive = lit;
	inputData.diffuse = !lit;
}

void cv_endFragment(inout cv_FragmentOutput outputData) {
	// NOOP
}
