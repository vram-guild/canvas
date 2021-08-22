/****************************************************************
 * frex:shaders/api/fragment.glsl - Canvas Implementation
 ***************************************************************/

in vec4 frx_vertex;
in vec2 frx_texcoord;
in vec4 frx_vertexColor;

#ifndef DEPTH_PASS
in vec3 frx_vertexNormal;
in vec4 frx_vertexLight;
in vec4 frx_var0;
in vec4 frx_var1;
in vec4 frx_var2;
in vec4 frx_var3;
in float frx_distance;
#endif

vec4 frx_sampleColor;
vec4 frx_fragColor;
float frx_fragReflectance;
vec3 frx_fragNormal;
float frx_fragHeight;
float frx_fragRoughness;
float frx_fragEmissive;
vec4 frx_fragLight;

// For compatibility - do not use
struct frx_FragmentData {
	bool diffuse;
	bool ao;
};

frx_FragmentData compatData;