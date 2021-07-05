#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/

//#define CV_VF

#ifdef VERTEX_SHADER

	#ifdef CV_VF

		uniform samplerBuffer _cvu_vfColor;
		uniform samplerBuffer _cvu_vfUV;
		uniform isamplerBuffer _cvu_vfVertex;
		uniform samplerBuffer _cvu_vfLight;

		in int in_header_vf;
		in int in_vertex_vf;
		in int in_light_vf;
		in int in_color_vf;
		in int in_uv_vf;
		in int in_pad0_vf;
		in int in_pad1_vf;

		#define in_color texelFetch(_cvu_vfColor, in_color_vf)
		#define in_uv texelFetch(_cvu_vfUV, in_uv_vf).rg

		vec3 in_vertex;
		vec3 in_normal;
		int in_material;
		vec2 in_lightmap;
		float in_ao;

		void _cv_prepareForVertex() {
			in_material = (in_header_vf >> 16) & 0xFFFF;
			ivec4 vfv = texelFetch(_cvu_vfVertex, in_vertex_vf);
			in_vertex = intBitsToFloat(vfv.xyz) + vec3(in_header_vf & 0xF, (in_header_vf >> 4) & 0xF, (in_header_vf >> 8) & 0xF);
			in_normal = (vec3(vfv.w & 0xFF, (vfv.w >> 8) & 0xFF, (vfv.w >> 16) & 0xFF) - 127.0) / 127.0;
			vec4 light = texelFetch(_cvu_vfLight, in_light_vf);
			in_lightmap = light.rg * 256.0;
			in_ao = light.b;
		}

	#else

		in vec3 in_vertex;
		in vec4 in_color;
		in vec2 in_uv;
		in vec2 in_lightmap;
		in int in_material;
		in vec3 in_normal;
		in float in_ao;

		void _cv_prepareForVertex() { }

	#endif

#endif
