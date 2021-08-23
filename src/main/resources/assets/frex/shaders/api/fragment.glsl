/****************************************************************
 * frex:shaders/api/fragment.glsl - Canvas Implementation
 ***************************************************************/

in vec4 frx_vertex;
in vec2 frx_texcoord;
in vec4 frx_vertexColor;

vec4 frx_sampleColor;
vec4 frx_fragColor;

#ifndef DEPTH_PASS
in vec3 frx_vertexNormal;
in vec3 frx_vertexLight;
in vec4 frx_var0;
in vec4 frx_var1;
in vec4 frx_var2;
in vec4 frx_var3;
in float frx_distance;

float frx_fragReflectance;
vec3 frx_fragNormal;
float frx_fragHeight;
float frx_fragRoughness;
float frx_fragEmissive;
vec3 frx_fragLight;
float frx_fragAo;
bool frx_fragEnableAo;
bool frx_fragEnableDiffuse;
#endif

#define _CV_FRAGMENT_COMPAT

#ifdef _CV_FRAGMENT_COMPAT
// For compatibility - do not use
struct frx_FragmentData {
	vec4 spriteColor;
	vec4 vertexColor;
};

frx_FragmentData compatData;
#endif