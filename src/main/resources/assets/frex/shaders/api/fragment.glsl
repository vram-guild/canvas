/****************************************************************
 * frex:shaders/api/fragment.glsl - Canvas Implementation
 ***************************************************************/

in vec4 frx_vertex;
in vec2 frx_texcoord;
in vec4 frx_vertexColor;
in vec3 frx_vertexNormal;

vec4 frx_sampleColor;
vec4 frx_fragColor;

#ifndef DEPTH_PASS
in vec3 frx_vertexLight;
in vec4 frx_var0;
in vec4 frx_var1;
in vec4 frx_var2;
in vec4 frx_var3;
in float frx_distance;

vec3 frx_fragLight;
bool frx_fragEnableAo;
bool frx_fragEnableDiffuse;

#ifdef PBR_ENABLED
vec4 _cv_fragNormalHeight;
#define frx_fragNormal _cv_fragNormalHeight.xyz
#define frx_fragHeight _cv_fragNormalHeight.w

vec4 _cv_fragVaria;
#define frx_fragReflectance _cv_fragVaria.x
#define frx_fragRoughness _cv_fragVaria.y
#define frx_fragEmissive _cv_fragVaria.z
#define frx_fragAo _cv_fragVaria.w
#else
float frx_fragEmissive;
#endif // PBR

#endif  // not depth pass

#define _CV_FRAGMENT_COMPAT

#ifdef _CV_FRAGMENT_COMPAT
// For compatibility - do not use
struct frx_FragmentData {
	vec4 spriteColor;
	vec4 vertexColor;
};

frx_FragmentData compatData;
#endif