#include canvas:shaders/pipeline/standard_header.frag
#include canvas:shaders/pipeline/common.frag

/******************************************************
  canvas:shaders/pipeline/standard.frag
******************************************************/

void frx_writePipelineFragment(in frx_FragmentData fragData) {
	vec4 baseColor = p_writeBaseColorAndDepth(fragData);
	fragColor[TARGET_EMISSIVE] = vec4(fragData.emissivity * baseColor.a, 0.0, 0.0, 1.0);
}
