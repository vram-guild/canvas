#define TRUE 1
#define FALSE 0

#define FLAG_EMISSIVE           0 // 1 for emissive material
#define FLAG_DISABLE_DIFFUSE    1 // 1 if diffuse shade should not be applied
#define FLAG_DISABLE_AO         2 // 1 if ao shade should not be applied
#define FLAG_CUTOUT             3 // 1 if cutout layer - will only be set in base, non-translucent materials
#define FLAG_UNMIPPED           4 // 1 if LOD disabled - only set in conjunction with cutout
#define FLAG_RESERVED_5         5
#define FLAG_RESERVED_6         6
#define FLAG_RESERVED_7         7
