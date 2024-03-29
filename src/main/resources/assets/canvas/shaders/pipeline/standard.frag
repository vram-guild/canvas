#include canvas:shaders/pipeline/standard_header.frag
#include canvas:shaders/pipeline/common.frag

/******************************************************
  canvas:shaders/pipeline/standard.frag
******************************************************/

void frx_pipelineFragment() {
	vec4 baseColor = p_writeBaseColorAndDepth();
	fragColor[TARGET_EMISSIVE] = vec4(frx_fragEmissive * baseColor.a, 0.0, 0.0, 1.0);
}
