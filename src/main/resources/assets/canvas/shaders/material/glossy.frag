#include frex:shaders/api/fragment.glsl

/******************************************************
  canvas:shaders/material/glossy.frag
******************************************************/

void frx_startFragment(inout frx_FragmentData fragData) {
    fragData.roughness = 0.0;
}
