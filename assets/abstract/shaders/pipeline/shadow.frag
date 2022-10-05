/******************************************************
  abstract:shaders/pipeline/shadow.frag
******************************************************/

void frx_pipelineFragment() {
	gl_FragDepth = gl_FragCoord.z;
}
