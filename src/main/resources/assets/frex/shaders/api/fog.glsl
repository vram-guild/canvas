#include canvas:shaders/internal/world.glsl

/******************************************************
  frex:shaders/api/fog.glsl
******************************************************/

#define  FOG_LINEAR  0
#define  FOG_EXP     1
#define  FOG_EXP2    2
#define  FOG_DISABLE 3

/**
 * Indicates the OpenGL fixed-function fog type game currently expects
 * Pipelines will likely want these to control fog effects but aren't
 * required to duplicate fixed-function fog exactly.
 *
 * Returns one of the FOG_ constants defined above.
 */
int frx_fogMode() {
	return _cvu_fog_mode;
}
