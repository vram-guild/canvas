/******************************************************
  frex:shaders/lib/bitwise.glsl

  Utilities for bitwise math.

  GLSL 120 unfortunately lacks bitwise operations
  so we need to emulate them unless an extension is active.

  Renderer is responsible for providing the populated uniform.
******************************************************/

/**
 * Returns the value (0-1) of the indexed bit (0-7)
 * within a float value that represents a single byte (0-255).
 */
float frx_bitValue(uint byteValue, int bitIndex) {
	// Mesa drivers won't handle implicit conversion of uint to float on return
	return float((byteValue >> bitIndex) & 1u);
}
