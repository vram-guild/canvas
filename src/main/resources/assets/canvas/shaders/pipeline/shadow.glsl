#include frex:shaders/api/context.glsl
#include canvas:shadow_options

/******************************************************
  canvas:shaders/pipeline/shadow.glsl
******************************************************/

vec2 shadowMapSize = vec2(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);

// Adapted from https://github.com/TheRealMJP/Shadows - MIT License
vec2 computeReceiverPlaneDepthBias(vec3 texCoordDX, vec3 texCoordDY) {
	vec2 biasUV;
    biasUV.x = texCoordDY.y * texCoordDX.z - texCoordDX.y * texCoordDY.z;
    biasUV.y = texCoordDX.x * texCoordDY.z - texCoordDY.x * texCoordDX.z;
    biasUV *= 1.0f / ((texCoordDX.x * texCoordDY.y) - (texCoordDX.y * texCoordDY.x));
    return biasUV;
}

//-------------------------------------------------------------------------------------------------
// Helper function for pcfSampleOptimizedPCF
// Adapted from https://github.com/TheRealMJP/Shadows - MIT License
//-------------------------------------------------------------------------------------------------
float pcfSample(in vec2 base_uv, in float u, in float v, in vec2 shadowMapSizeInv, in float cascade,  in float depth, in vec2 receiverPlaneDepthBias) {

    vec2 uv = base_uv + vec2(u, v) * shadowMapSizeInv;
    float z = depth + dot(vec2(u, v) * shadowMapSizeInv, receiverPlaneDepthBias);

	return shadow2DArray(frxs_shadowMap, vec4(uv, cascade, z)).x;
}

//-------------------------------------------------------------------------------------------------
// The method used in The Witness
// Adapted from https://github.com/TheRealMJP/Shadows - MIT License
// Returns 0 for fully shaded, 1 for full exposure.
//-------------------------------------------------------------------------------------------------
float sampleShadowPCF(in vec3 shadowPos, in float cascade) {
	vec3 shadowPosDX = dFdx(shadowPos);
	vec3 shadowPosDY = dFdy(shadowPos);

    float lightDepth = shadowPos.z;

	vec2 texelSize = 1.0f / shadowMapSize;
	vec2 receiverPlaneDepthBias = computeReceiverPlaneDepthBias(shadowPosDX, shadowPosDY);

	// Static depth biasing to make up for incorrect fractional sampling on the shadow map grid
	float fractionalSamplingError = 2 * dot(vec2(1.0f, 1.0f) * texelSize, abs(receiverPlaneDepthBias));
	lightDepth -= min(fractionalSamplingError, 0.01f);

    vec2 uv = shadowPos.xy * shadowMapSize; // 1 unit - 1 texel

    vec2 shadowMapSizeInv = 1.0 / shadowMapSize;

    vec2 base_uv;
    base_uv.x = floor(uv.x + 0.5);
    base_uv.y = floor(uv.y + 0.5);

    float s = (uv.x + 0.5 - base_uv.x);
    float t = (uv.y + 0.5 - base_uv.y);

    base_uv -= vec2(0.5, 0.5);
    base_uv *= shadowMapSizeInv;

    float sum = 0;

    #if SHADOW_FILTER_SIZE == PCF_SIZE_SMALL
    	return shadow2DArray(frxs_shadowMap, vec4(shadowPos.xy, cascade, shadowPos.z)).x;
    #elif SHADOW_FILTER_SIZE == PCF_SIZE_MEDIUM

        float uw0 = (3 - 2 * s);
        float uw1 = (1 + 2 * s);

        float u0 = (2 - s) / uw0 - 1;
        float u1 = s / uw1 + 1;

        float vw0 = (3 - 2 * t);
        float vw1 = (1 + 2 * t);

        float v0 = (2 - t) / vw0 - 1;
        float v1 = t / vw1 + 1;

        sum += uw0 * vw0 * pcfSample(base_uv, u0, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw0 * pcfSample(base_uv, u1, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw0 * vw1 * pcfSample(base_uv, u0, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw1 * pcfSample(base_uv, u1, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);

        return sum * 1.0f / 16;

    #elif SHADOW_FILTER_SIZE == PCF_SIZE_LARGE

        float uw0 = (4 - 3 * s);
        float uw1 = 7;
        float uw2 = (1 + 3 * s);

        float u0 = (3 - 2 * s) / uw0 - 2;
        float u1 = (3 + s) / uw1;
        float u2 = s / uw2 + 2;

        float vw0 = (4 - 3 * t);
        float vw1 = 7;
        float vw2 = (1 + 3 * t);

        float v0 = (3 - 2 * t) / vw0 - 2;
        float v1 = (3 + t) / vw1;
        float v2 = t / vw2 + 2;

        sum += uw0 * vw0 * pcfSample(base_uv, u0, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw0 * pcfSample(base_uv, u1, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw2 * vw0 * pcfSample(base_uv, u2, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);

        sum += uw0 * vw1 * pcfSample(base_uv, u0, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw1 * pcfSample(base_uv, u1, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw2 * vw1 * pcfSample(base_uv, u2, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);

        sum += uw0 * vw2 * pcfSample(base_uv, u0, v2, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw2 * pcfSample(base_uv, u1, v2, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw2 * vw2 * pcfSample(base_uv, u2, v2, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);

        return sum * 1.0f / 144;

    #else // PCF_SIZE_EXTRA

        float uw0 = (5 * s - 6);
        float uw1 = (11 * s - 28);
        float uw2 = -(11 * s + 17);
        float uw3 = -(5 * s + 1);

        float u0 = (4 * s - 5) / uw0 - 3;
        float u1 = (4 * s - 16) / uw1 - 1;
        float u2 = -(7 * s + 5) / uw2 + 1;
        float u3 = -s / uw3 + 3;

        float vw0 = (5 * t - 6);
        float vw1 = (11 * t - 28);
        float vw2 = -(11 * t + 17);
        float vw3 = -(5 * t + 1);

        float v0 = (4 * t - 5) / vw0 - 3;
        float v1 = (4 * t - 16) / vw1 - 1;
        float v2 = -(7 * t + 5) / vw2 + 1;
        float v3 = -t / vw3 + 3;

        sum += uw0 * vw0 * pcfSample(base_uv, u0, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw0 * pcfSample(base_uv, u1, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw2 * vw0 * pcfSample(base_uv, u2, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw3 * vw0 * pcfSample(base_uv, u3, v0, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);

        sum += uw0 * vw1 * pcfSample(base_uv, u0, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw1 * pcfSample(base_uv, u1, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw2 * vw1 * pcfSample(base_uv, u2, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw3 * vw1 * pcfSample(base_uv, u3, v1, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);

        sum += uw0 * vw2 * pcfSample(base_uv, u0, v2, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw2 * pcfSample(base_uv, u1, v2, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw2 * vw2 * pcfSample(base_uv, u2, v2, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw3 * vw2 * pcfSample(base_uv, u3, v2, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);

        sum += uw0 * vw3 * pcfSample(base_uv, u0, v3, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw1 * vw3 * pcfSample(base_uv, u1, v3, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw2 * vw3 * pcfSample(base_uv, u2, v3, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);
        sum += uw3 * vw3 * pcfSample(base_uv, u3, v3, shadowMapSizeInv, cascade, lightDepth, receiverPlaneDepthBias);

        return sum * 1.0f / 2704;

    #endif
}
