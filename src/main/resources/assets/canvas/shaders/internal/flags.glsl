#include canvas:shaders/api/context.glsl
#include canvas:shaders/lib/bitwise.glsl

/******************************************************
  canvas:shaders/internal/flags.glsl
******************************************************/

#define _CV_FLAG_EMISSIVE           0 // 1 for emissive material
#define _CV_FLAG_DISABLE_DIFFUSE    1 // 1 if diffuse shade should not be applied
#define _CV_FLAG_DISABLE_AO         2 // 1 if ao shade should not be applied
#define _CV_FLAG_CUTOUT             3 // 1 if cutout layer - will only be set in base, non-translucent materials
#define _CV_FLAG_UNMIPPED           4 // 1 if LOD disabled - only set in conjunction with cutout
#define _CV_FLAG_RESERVED_5         5
#define _CV_FLAG_RESERVED_6         6
#define _CV_FLAG_RESERVED_7         7

#if USE_FLAT_VARYING
    // may be faster when available and
    // prevents problems on some NVidia cards/drives
    flat varying float _cvv_flags;
#else
    // flat no available on mesa drivers
    invariant varying float _cvv_flags;
#endif

float _cv_getFlag(int flagId) {
	return bitValue(_cvv_flags, flagId);
}
