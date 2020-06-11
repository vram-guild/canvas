struct cv_FragmentInput {
	vec4 spriteColor;
	vec4 vertexColor;
	bool emissive;
	bool diffuse;
	//vec3 vertexNormal;
};

struct cv_FragmentOutput {
	vec4 baseColor;
	bool emissive;
	//vec3 normal;
};
