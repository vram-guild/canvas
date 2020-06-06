#version 120
#include canvas:shaders/std_fragment_lib.glsl

void main() {
    gl_FragColor = fog(diffuseColor());
}
