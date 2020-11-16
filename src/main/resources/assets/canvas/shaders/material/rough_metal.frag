#include frex:shaders/api/fragment.glsl

/******************************************************
  canvas:shaders/material/rough_metal.frag
******************************************************/

void frx_startFragment(inout frx_FragmentData fragData) {
    fragData.metal = 1.0;
    fragData.roughness = 0.5;
}
