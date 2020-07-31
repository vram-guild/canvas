/******************************************************
  frex:shaders/lib/bitwise.glsl

  Utilities for bitwise math.

  GLSL 120 unfortunately lacks bitwise operations
  so we need to emulate them unless an extension is active.

  Renderer is responsible for providing the populated uniform.
******************************************************/

uniform float[8] _fru_bitwise_divisors; // = float[8](0.5, 0.25, 0.125, 0.0625, 0.03125, 0.015625, 0.0078125, 0.00390625);

/**
 * Returns the value (0-1) of the indexed bit (0-7)
 * within a float value that represents a single byte (0-255).
 */
float frx_bitValue(float byteValue, int bitIndex) {
    return floor(fract(byteValue * _fru_bitwise_divisors[bitIndex]) * 2.0);
}
