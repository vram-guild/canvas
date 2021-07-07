#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/


#ifdef VERTEX_SHADER

	#ifdef CV_VF
		flat out vec4 _cv_modelOrigin;

		uniform samplerBuffer _cvu_vfColor;
		uniform samplerBuffer _cvu_vfUV;
		uniform isamplerBuffer _cvu_vfVertex;
		uniform samplerBuffer _cvu_vfLight;
		uniform isamplerBuffer _cvu_vfQuads;
		uniform isamplerBuffer _cvu_vfRegions;
		uniform usamplerBuffer _cvu_vfQuadRegions;

		// x was region quad offset - no longer used
		// y is base region index - will remain
		// z is region index
		uniform ivec3 _cvu_vf_hack;

		int in_material;
		vec3 in_vertex;
		vec3 in_normal;
		vec4 in_color;
		vec2 in_uv;
		vec2 in_lightmap;
		float in_ao;


		void _cv_prepareForVertex() {
			// WIP: get correct region index
			ivec4 region = texelFetch(_cvu_vfRegions, _cvu_vf_hack.y + _cvu_vf_hack.z);

			_cv_modelOrigin = vec4(region.xyz, 0.0);

			int quadID = gl_VertexID / 6;
			int v = gl_VertexID - quadID * 6;
			v = v < 3 ? v : ((v - 1) & 3);

			ivec4 q = texelFetch(_cvu_vfQuads, region.w + quadID);
			in_material = (q.x >> 12) & 0xFFFF;

			ivec4 vfv = texelFetch(_cvu_vfVertex, ((q.y & 0xFFFFFF) << 2) + v);
			in_vertex = intBitsToFloat(vfv.xyz) + vec3(q.x & 0xF, (q.x >> 4) & 0xF, (q.x >> 8) & 0xF);
			in_normal = (vec3(vfv.w & 0xFF, (vfv.w >> 8) & 0xFF, (vfv.w >> 16) & 0xFF) - 127.0) / 127.0;

			in_color = texelFetch(_cvu_vfColor, ((q.z & 0xFFFFFF) << 2) + v);

			in_uv = texelFetch(_cvu_vfUV, ((q.w & 0xFFFFFF) << 2) + v).rg;

			int lightIndex = ((q.y >> 24) & 0xFF) | ((q.z >> 16) & 0xFF00) | ((q.w >> 8) & 0xFF0000);
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
