#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl
#include frex:shaders/api/world.glsl
#include frex:shaders/lib/noise/cellular2x2x2.glsl

/******************************************************
  canvas:shaders/material/lava.frag
******************************************************/

// Color temperature computations are based on following sources, with much appreciation:
//
// Tanner Helland: How to Convert Temperature (K) to RGB: Algorithm and Sample Code
// http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
//
// Neil Bartlett: COLOR TEMPERATURE CONVERSION OF HOMESTAR.IO
// http://www.zombieprototypes.com/?p=210

// Estimates green color component for black body radiation at input temperature.
// For range we are using, can assume red component is 1.0
float bbrGreen(float kelvin)
{
    const float a = -155.25485562709179;
    const float b = -0.44596950469579133;
    const float c = 104.49216199393888;
    float x = (kelvin / 100.0) - 2;
    return (a + b * x + c * log(x)) / 255.0;
}

// Estimates blue color component for black body radiation at input temperature.
// For range we are using, can assume red component is 1.0
float bbrBlue(float kelvin)
{
	if(kelvin < 2000.0) return 0.0;
    const float a = -254.76935184120902;
    const float b = 0.8274096064007395;
    const float c = 115.67994401066147;
    float x = (kelvin / 100.0) - 10.0;
    return (a + b * x + c * log(x)) / 255.0;
}

void frx_startFragment(inout frx_FragmentData fragData) {
	float t = frx_renderSeconds();
	vec2 uv = frx_var0.xy * 4.00 + t * frx_var0.zw;
	float n = cellular2x2x2(vec3(uv.xy, t * 0.2)).x;
	float v = 700 + n * 2000;
	fragData.spriteColor = vec4(1.0, bbrGreen(v), bbrBlue(v), 1.0);
}
