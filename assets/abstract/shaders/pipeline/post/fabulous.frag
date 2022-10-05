#include frex:shaders/api/header.glsl
#include abstract:shaders/pipeline/pipeline.glsl

/******************************************************
  abstract:shaders/pipeline/post/fabulous.frag
******************************************************/

// a slightly cleaned up version of Mojang's transparency.fsh
uniform sampler2D diffuseColor;
uniform sampler2D diffuseDepth;
uniform sampler2D translucentColor;
uniform sampler2D translucentDepth;
uniform sampler2D entityColor;
uniform sampler2D entityDepth;
uniform sampler2D particleColor;
uniform sampler2D particleDepth;
uniform sampler2D weatherColor;
uniform sampler2D weatherDepth;
uniform sampler2D cloudsColor;
uniform sampler2D cloudsDepth;

in vec2 _cvv_texcoord;
out vec4 fragColor;

#define NUM_LAYERS 6

vec4 color_layers[NUM_LAYERS];
float depth_layers[NUM_LAYERS];
int active_layers = 0;

void try_insert(vec4 color, float depth) {
	if (color.a == 0.0) {
		return;
	}

	color_layers[active_layers] = color;
	depth_layers[active_layers] = depth;

	int target = active_layers++;
	int probe = target - 1;

	while (target > 0 && depth_layers[target] > depth_layers[probe]) {
		float probeDepth = depth_layers[probe];
		depth_layers[probe] = depth_layers[target];
		depth_layers[target] = probeDepth;

		vec4 probeColor = color_layers[probe];
		color_layers[probe] = color_layers[target];
		color_layers[target] = probeColor;

		target = probe--;
	}
}

vec3 blend(vec3 dst, vec4 src) {
	return (dst * (1.0 - src.a)) + src.rgb;
}

void main() {
	color_layers[0] = vec4(texture(diffuseColor, _cvv_texcoord).rgb, 1.0);
	depth_layers[0] = texture(diffuseDepth, _cvv_texcoord).r;
	active_layers = 1;

	try_insert(texture(translucentColor, _cvv_texcoord), texture(translucentDepth, _cvv_texcoord).r);
	try_insert(texture(entityColor, _cvv_texcoord), texture(entityDepth, _cvv_texcoord).r);
	try_insert(texture(particleColor, _cvv_texcoord), texture(particleDepth, _cvv_texcoord).r);
	try_insert(texture(weatherColor, _cvv_texcoord), texture(weatherDepth, _cvv_texcoord).r);
	try_insert(texture(cloudsColor, _cvv_texcoord), texture(cloudsDepth, _cvv_texcoord).r);

	vec3 texelAccum = color_layers[0].rgb;

	for (int i = 1; i < active_layers; ++i) {
		texelAccum = blend(texelAccum, color_layers[i]);
	}

	fragColor = vec4(texelAccum.rgb, 1.0);
}
