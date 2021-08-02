#include canvas:shaders/pipeline/basic_header.frag
#include canvas:shaders/pipeline/common.frag

/******************************************************
  canvas:shaders/pipeline/basic.frag
******************************************************/

void frx_writePipelineFragment(in frx_FragmentData fragData) {
	p_writeBaseColorAndDepth(fragData);
}
