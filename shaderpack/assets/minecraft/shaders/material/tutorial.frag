#include canvas:shaders/api/fragment.glsl
#include canvas:shaders/lib/math.glsl
#include canvas:shaders/api/world.glsl

// holds our noise coordinates from the vertex shader
varying vec2 v_noise_uv;

void cv_startFragment(inout cv_FragmentData fragData) {
	// modify appearance where stone texture is lighter in color
	if (cv_luminance(fragData.spriteColor.rgb) > 0.5) {
		// get a time value we can use for animation
		float time = cv_renderSeconds();

		// use an animated noise function to mix a pastel blue/red color
		float color_weight = cv_noise2dt(v_noise_uv * 2.0, time);

		// mix 'em up!
		vec4 highlight = mix(vec4(1.0, 0.7, 1.0, 1.0), vec4(0.7, 1.0, 1.0, 1.0), color_weight);

		// call animated noise function with noise coordinates scaled and shifted
		float blend_weight = cv_noise2dt(v_noise_uv * 4.0 - 16.0, time * 2.0);

		// mix with the stone texture colors
		fragData.spriteColor = mix(fragData.spriteColor, highlight, blend_weight);

		// make these fragments fully lit
		fragData.emissivity = 1.0;
		fragData.ao = false;
		fragData.diffuse = false;
	}
}
