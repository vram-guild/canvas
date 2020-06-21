/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/

vec2 _cv_textureCoord(vec2 coordIn, int matrixIndex) {
	vec4 temp = gl_TextureMatrix[matrixIndex] * coordIn.xyxy;
	return temp.xy;
}

vec3 _cv_diffuseNormal(vec4 viewCoord, vec3 normal) {
//#if CONTEXT == CONTEXT_ITEM_WORLD
//    // TODO: Need to transform normals for in-world items to get directionally correct shading.
//    // Problem is that we don't have a MVM for the lights. Will need to capture that
//    // or transform the lights on CPU side, which is probably the better deal.
//    return normal;
//#else
    return normal;
//#endif
}
