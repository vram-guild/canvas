#include frex:shaders/api/context.glsl
#include frex:shaders/lib/bitwise.glsl

/******************************************************
  canvas:shaders/internal/flags.glsl
******************************************************/

#define _CV_FLAG_EMISSIVE           0// 1 for emissive material
#define _CV_FLAG_DISABLE_DIFFUSE    1// 1 if diffuse shade should not be applied
#define _CV_FLAG_DISABLE_AO         2// 1 if ao shade should not be applied
#define _CV_FLAG_CUTOUT_LOW         3// low cutout bit
#define _CV_FLAG_CUTOUT_HIGH 		4// high cutout bit
#define _CV_FLAG_UNMIPPED           5// 1 if LOD disabled - only set in conjunction with cutout
#define _CV_FLAG_HURT_OVERLAY       6// 1 if should render red hurt overlay
#define _CV_FLAG_FLASH_OVERLAY      7// 1 if should render white flash overlay

#define _CV_CUTOUT_SHIFT 3u
#define _CV_CUTOUT_MASK 3u
#define _CV_CUTOUT_NONE 0u
#define _CV_CUTOUT_HALF 1u
#define _CV_CUTOUT_TENTH 2u
#define _CV_CUTOUT_ZERO 3u

#define _CV_TRANSLUCENT_CUTOUT_THRESHOLD 0.003921569

#ifdef VERTEX_SHADER
	flat out uint _cvv_flags;
#else
	flat in uint _cvv_flags;
#endif

float _cv_getFlag(int flagId) {
	return frx_bitValue(_cvv_flags, flagId);
}

float _cv_cutoutThreshold() {
	switch((_cvv_flags >> _CV_CUTOUT_SHIFT) & _CV_CUTOUT_MASK) {
	default:
	case _CV_CUTOUT_NONE:
		return -1.0;
	case _CV_CUTOUT_HALF:
		return 0.5;
	case _CV_CUTOUT_TENTH:
		return 0.1;
	case _CV_CUTOUT_ZERO:
		return 0.0;
	}
}
