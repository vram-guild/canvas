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
		in int in_color_vf;
		in int in_uv_vf;

		int in_material;
		vec3 in_vertex;
		vec3 in_normal;
		vec4 in_color;
		vec2 in_uv;
		vec2 in_lightmap;
		float in_ao;

		void _cv_prepareForVertex() {
			in_material = (in_header_vf >> 12) & 0xFFFF;
			int v = (in_header_vf >> 28) & 3;

			ivec4 vfv = texelFetch(_cvu_vfVertex, ((in_vertex_vf & 0xFFFFFF) << 2) + v);
			in_vertex = intBitsToFloat(vfv.xyz) + vec3(in_header_vf & 0xF, (in_header_vf >> 4) & 0xF, (in_header_vf >> 8) & 0xF);
			in_normal = (vec3(vfv.w & 0xFF, (vfv.w >> 8) & 0xFF, (vfv.w >> 16) & 0xFF) - 127.0) / 127.0;

			in_color = texelFetch(_cvu_vfColor, ((in_color_vf & 0xFFFFFF) << 2) + v);

			in_uv = texelFetch(_cvu_vfUV, ((in_uv_vf & 0xFFFFFF) << 2) + v).rg;

			int lightIndex = ((in_vertex_vf >> 24) & 0xFF) | ((in_color_vf >> 16) & 0xFF00) | ((in_uv_vf >> 8) & 0xFF0000);
			vec4 light = texelFetch(_cvu_vfLight, (lightIndex << 2) + v);
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
