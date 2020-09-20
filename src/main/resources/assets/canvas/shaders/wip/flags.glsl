#include frex:shaders/wip/api/context.glsl
#include frex:shaders/lib/bitwise.glsl

/******************************************************
  canvas:shaders/internal/flags.glsl
******************************************************/

#define _CV_FLAG_EMISSIVE           0// 1 for emissive material
#define _CV_FLAG_DISABLE_DIFFUSE    1// 1 if diffuse shade should not be applied
#define _CV_FLAG_DISABLE_AO         2// 1 if ao shade should not be applied
#define _CV_FLAG_CUTOUT             3// 1 if cutout layer - will only be set in base, non-translucent materials
#define _CV_FLAG_UNMIPPED           4// 1 if LOD disabled - only set in conjunction with cutout
#define _CV_FLAG_CUTOUT_10          5// 1 if "10%" cutout threshold (Yarn calls it 10, it's not actually 10%
#define _CV_FLAG_HURT_OVERLAY       6// 1 if should render red hurt overlay
#define _CV_FLAG_FLASH_OVERLAY      7// 1 if should render white flash overlay

#define _CV_CUTOUT_10_THRESHOLD 	0.003921569

#ifdef USE_FLAT_VARYING
// may be faster when available and
// prevents problems on some NVidia cards/drives
flat varying float _cvv_flags;
#else
// flat no available on mesa drivers
invariant varying float _cvv_flags;
#endif

float _cv_getFlag(int flagId) {
	return frx_bitValue(_cvv_flags, flagId);
}
