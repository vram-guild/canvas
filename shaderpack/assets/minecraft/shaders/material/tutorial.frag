#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/api/world.glsl

void frx_startFragment(inout frx_FragmentData fragData) {
	// modify appearance where stone texture is lighter in color
	if (frx_luminance(fragData.spriteColor.rgb) > 0.5) {
		// get a time value we can use for animation
		float time = frx_renderSeconds();

		// use an animated noise function to mix a pastel blue/red color
		float color_weight = frx_noise2dt(frx_var0.xy * 2.0, time);

		// mix 'em up!
		vec4 highlight = mix(vec4(1.0, 0.7, 1.0, 1.0), vec4(0.7, 1.0, 1.0, 1.0), color_weight);

		// call animated noise function with noise coordinates scaled and shifted
		float blend_weight = frx_noise2dt(frx_var0.xy * 4.0 - 16.0, time * 2.0);

		// mix with the stone texture colors
		fragData.spriteColor = mix(fragData.spriteColor, highlight, blend_weight);

		// make these fragments fully lit
		fragData.emissivity = 1.0;
		fragData.ao = false;
		fragData.diffuse = false;
	}
}
