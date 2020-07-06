#version 120
#extension GL_EXT_gpu_shader4 : enable

// TODO: renamae and expose - needed for sampling
uniform sampler2D _cvu_textures;
uniform sampler2D _cvu_lightmap;
