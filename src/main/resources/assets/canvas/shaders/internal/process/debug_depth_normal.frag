#include canvas:shaders/internal/process/header.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/lib/sample.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/internal/process/debug_depth_normal.frag
******************************************************/
uniform sampler2D _cvu_normal;
uniform sampler2D _cvu_depth;
uniform float cvu_intensity;
uniform vec2 _cvu_distance;

varying vec2 _cvv_texcoord;

vec4 visualizeDepth(){
	float z = texture2D(_cvu_depth, _cvv_texcoord).r;
	float near = _cvu_distance.x;
	float far = _cvu_distance.y;
	float c = (2.0 * near) / (far + near - z * (far - near));
	return vec4(c);
}

vec4 visualizeNormal(){
	return vec4(texture2D(_cvu_normal, _cvv_texcoord).xyz, 1.0);
}

void main() {
	if(cvu_intensity > 0.5){
		gl_FragData[0] = visualizeDepth();
	} else {
		gl_FragData[0] = visualizeNormal();
	}
}
