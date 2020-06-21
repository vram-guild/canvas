/******************************************************
  canvas:shaders/api/world.glsl
******************************************************/

#define WORLD_EFFECT_MODIFIER 0
#define WORLD_NIGHT_VISION 1
#define WORLD_EFFECTIVE_INTENSITY 2
#define WORLD_AMBIENT_INTENSITY 3
#define WORLD_HAS_SKYLIGHT 4
#define WOLRD_RESERVED 5
#define WOLRD_MOON_SIZE 6

#define DIMENSION_OVERWORLD 1
#define DIMENSION_NETHER 0
#define DIMENSION_END 2

uniform float[8] _cvu_world;

float effectModifier() {
    return _cvu_world[WORLD_EFFECT_MODIFIER];
}
