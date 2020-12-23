/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.pipeline;

import net.minecraft.util.Identifier;

class PipelineConfig {
	static class PipelineParam {
		String name;
		float minVal;
		float maxVal;
		float defaultVal;
		float currentVal;

		static PipelineParam of(
			String name,
			float minVal,
			float maxVal,
			float defaultVal
		) {
			final PipelineParam result = new PipelineParam();
			result.name = name;
			result.minVal = minVal;
			result.maxVal = maxVal;
			result.defaultVal = defaultVal;
			result.currentVal = defaultVal;
			return result;
		}

		static PipelineParam[] array(PipelineParam... params) {
			return params;
		}
	}

	static class ImageConfig {
		Identifier id;
		boolean hdr;
		boolean blur;
		// WIP: make more configurable
		int lod;

		static ImageConfig of(Identifier id, boolean hdr, boolean blur, int lod) {
			final ImageConfig result = new ImageConfig();
			result.id = id;
			result.hdr = hdr;
			result.lod = lod;
			result.blur = blur;
			return result;
		}

		static ImageConfig[] array(ImageConfig... configs) {
			return configs;
		}
	}

	static class AttachmentConfig {
		Identifier image;
		int lod;
		int clearColor;

		static AttachmentConfig of(
			Identifier image,
			int clearColor,
			int lod
		) {
			final AttachmentConfig result = new AttachmentConfig();
			result.image = image;
			result.lod = lod;
			result.clearColor = clearColor;
			return result;
		}

		static AttachmentConfig of(
			String image,
			int clearColor,
			int lod
		) {
			return of(new Identifier(image), lod, clearColor);
		}

		static AttachmentConfig[] array(AttachmentConfig... attachments) {
			return attachments;
		}
	}

	static class FramebufferConfig {
		Identifier id;
		AttachmentConfig[] attachments;

		static FramebufferConfig of(
			Identifier id,
			AttachmentConfig... attachments
		) {
			final FramebufferConfig result = new FramebufferConfig();
			result.id = id;
			result.attachments = attachments;
			return result;
		}

		static FramebufferConfig of(
			String id,
			AttachmentConfig... attachments
		) {
			return of(new Identifier(id), attachments);
		}

		static FramebufferConfig[] array(FramebufferConfig... configs) {
			return configs;
		}
	}

	static class SamplerConfig {
		Identifier texture;
		boolean gameTexture;

		static SamplerConfig of(
			Identifier texture,
			boolean gameTexture
		) {
			final SamplerConfig result = new SamplerConfig();
			result.texture = texture;
			result.gameTexture = gameTexture;
			return result;
		}

		static SamplerConfig of(
			String texture,
			boolean gameTexture,
			int samplerIndex
		) {
			return of(new Identifier(texture), gameTexture);
		}

		static SamplerConfig[] array(SamplerConfig... samplers) {
			return samplers;
		}
	}

	static class ShaderConfig {
		Identifier id;
		Identifier vertexSource;
		Identifier fragmentSource;
		String[] samplerNames;

		static ShaderConfig of(
				Identifier id,
				Identifier vertexSource,
				Identifier fragmentSource,
				String... samplerNames
		) {
			final ShaderConfig result = new ShaderConfig();
			result.id = id;
			result.vertexSource = vertexSource;
			result.fragmentSource = fragmentSource;
			result.samplerNames = samplerNames;
			return result;
		}

		static ShaderConfig of(
				Identifier id,
				String vertexSource,
				String fragmentSource,
				String... samplerNames
		) {
			return of(id, new Identifier(vertexSource), new Identifier(fragmentSource), samplerNames);
		}

		static ShaderConfig[] array(ShaderConfig... configs) {
			return configs;
		}
	}

	static class PassConfig {
		Identifier framebuffer;
		SamplerConfig[] samplers;
		Identifier shader;
		// for computing size
		int lod;

		//Image[] attachments, int[] samplerBinds, ProcessShader shader, Consumer<ProcessShader> activator

		static PassConfig of(
			Identifier framebuffer,
			SamplerConfig[] samplers,
			Identifier shader,
			int lod
		) {
			final PassConfig result = new PassConfig();
			result.framebuffer = framebuffer;
			result.samplers = samplers;
			result.shader = shader;
			result.lod = lod;
			return result;
		}

		static PassConfig of(
			String framebuffer,
			SamplerConfig[] samplers,
			Identifier shader,
			int lod
		) {
			return of(new Identifier(framebuffer), samplers, shader, lod);
		}

		static PassConfig of(
			String framebuffer,
			SamplerConfig[] samplers,
			String shader,
			int lod
		) {
			return of(new Identifier(framebuffer), samplers, new Identifier(shader), lod);
		}

		static PassConfig[] array(PassConfig... passes) {
			return passes;
		}
	}

	ImageConfig[] images;
	PipelineParam[] params;
	ShaderConfig[] shaders;
	FramebufferConfig[] framebuffers;

	PassConfig[] onWorldStart;
	PassConfig[] afterRenderHand;

	{
		params = PipelineParam.array(
			PipelineParam.of("bloom_intensity", 0.0f, 0.5f, 0.1f),
			PipelineParam.of("bloom_scale", 0.0f, 2.0f, 0.25f)
		);

		images = ImageConfig.array(
			ImageConfig.of(IMG_EMISSIVE, false, true, 0),
			ImageConfig.of(IMG_EMISSIVE_COLOR, false, true, 0),
			// don't want filtering when copy back from main
			ImageConfig.of(IMG_MAIN_COPY, false, false, 0),
			ImageConfig.of(IMG_BLOOM_DOWNSAMPLE, false, true, 6),
			ImageConfig.of(IMG_BLOOM_UPSAMPLE, false, true, 6)
		);

		shaders = ShaderConfig.array(
			ShaderConfig.of(PROG_COPY, "canvas:shaders/internal/process/copy.vert", "canvas:shaders/internal/process/copy.frag", "_cvu_input", "tex0"),

			ShaderConfig.of(
				PROG_EMISSIVE_COLOR,
				"canvas:shaders/internal/process/emissive_color.vert",
				"canvas:shaders/internal/process/emissive_color.frag",
				"_cvu_base", "_cvu_emissive"
			),

			ShaderConfig.of(
				PROG_BLOOM,
				"canvas:shaders/internal/process/bloom.vert",
				"canvas:shaders/internal/process/bloom.frag",
				"_cvu_base", "_cvu_bloom"
			),

			ShaderConfig.of(
				PROG_COPY_LOD,
				"canvas:shaders/internal/process/copy_lod.vert",
				"canvas:shaders/internal/process/copy_lod.frag",
				"_cvu_input"
			),

			ShaderConfig.of(
				PROG_DOWNSAMPLE,
				"canvas:shaders/internal/process/downsample.vert",
				"canvas:shaders/internal/process/downsample.frag",
				"_cvu_input"
			),

			ShaderConfig.of(
				PROG_UPSAMPLE,
				"canvas:shaders/internal/process/upsample.vert",
				"canvas:shaders/internal/process/upsample.frag",
				"_cvu_input", "cvu_prior"
			),

			ShaderConfig.of(
					PROG_UPSAMPLE_FIRST,
				"canvas:shaders/internal/process/upsample.vert",
				"canvas:shaders/internal/process/upsample_first.frag",
				"_cvu_input"
			)
		);

		framebuffers = FramebufferConfig.array(
			FramebufferConfig.of(
				"canvas:emissive",
				AttachmentConfig.array(AttachmentConfig.of("canvas:emissive", 0, 0))
			),

			FramebufferConfig.of(
				"canvas:main_copy",
				AttachmentConfig.array(AttachmentConfig.of(IMG_MAIN_COPY, 0, 0))
			),

			FramebufferConfig.of(
				"canvas:emissive_color",
				AttachmentConfig.array(AttachmentConfig.of(IMG_EMISSIVE_COLOR, 0, 0))
			),

			FramebufferConfig.of(
				"canvas:bloom_downsample_0",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 0))
			),

			FramebufferConfig.of(
				"canvas:bloom_downsample_1",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 1))
			),

			FramebufferConfig.of(
				"canvas:bloom_downsample_2",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 2))
			),

			FramebufferConfig.of(
				"canvas:bloom_downsample_3",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 3))
			),

			FramebufferConfig.of(
				"canvas:bloom_downsample_4",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 4))
			),

			FramebufferConfig.of(
				"canvas:bloom_downsample_5",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 5))
			),

			FramebufferConfig.of(
				"canvas:bloom_downsample_6",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 6))
			),

			FramebufferConfig.of(
				"canvas:bloom_upsample_6",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 6))
			),

			FramebufferConfig.of(
				"canvas:bloom_upsample_5",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 5))
			),

			FramebufferConfig.of(
				"canvas:bloom_upsample_4",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 4))
			),

			FramebufferConfig.of(
				"canvas:bloom_upsample_3",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 3))
			),

			FramebufferConfig.of(
				"canvas:bloom_upsample_2",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 2))
			),

			FramebufferConfig.of(
				"canvas:bloom_upsample_1",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 1))
			),

			FramebufferConfig.of(
				"canvas:bloom_upsample_0",
				AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 0))
			),

			FramebufferConfig.of(
				"canvas:bloom",
				AttachmentConfig.array(AttachmentConfig.of(IMG_MC_MAIN, 0, 0))
			)
		);

		onWorldStart = PassConfig.array(
			PassConfig.of("canvas:emissive", SamplerConfig.array(), ClearPass.CLEAR_ID, 0)
		);

		afterRenderHand = PassConfig.array(
			PassConfig.of(
				"canvas:main_copy",
				SamplerConfig.array(SamplerConfig.of(IMG_MC_MAIN, false)),
				PROG_COPY,
				0
			),

			PassConfig.of(
				"canvas:emissive_color",
				SamplerConfig.array(
						SamplerConfig.of(IMG_MC_MAIN, false),
						SamplerConfig.of(IMG_EMISSIVE, false)
				),
				PROG_EMISSIVE_COLOR,
				0
			),

			PassConfig.of(
				"canvas:bloom_downsample_0",
				SamplerConfig.array(SamplerConfig.of(IMG_EMISSIVE_COLOR, false)),
				PROG_DOWNSAMPLE,
				0
			),

			PassConfig.of(
				"canvas:bloom_downsample_1",
				SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
				PROG_DOWNSAMPLE,
				1
			),

			PassConfig.of(
				"canvas:bloom_downsample_2",
				SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
				PROG_DOWNSAMPLE,
				2
			),

			PassConfig.of(
				"canvas:bloom_downsample_3",
				SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
				PROG_DOWNSAMPLE,
				3
			),

			PassConfig.of(
				"canvas:bloom_downsample_4",
				SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
				PROG_DOWNSAMPLE,
				4
			),

			PassConfig.of(
				"canvas:bloom_downsample_5",
				SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
				PROG_DOWNSAMPLE,
				5
			),

			PassConfig.of(
				"canvas:bloom_downsample_6",
				SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
				PROG_DOWNSAMPLE,
				6
			),

			PassConfig.of(
				"canvas:bloom_upsample_6",
				SamplerConfig.array(
						SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)
				),
				PROG_UPSAMPLE_FIRST,
				6
			),

			PassConfig.of(
				"canvas:bloom_upsample_5",
				SamplerConfig.array(
						SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
						SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
				),
				PROG_UPSAMPLE,
				5
			),

			PassConfig.of(
				"canvas:bloom_upsample_4",
				SamplerConfig.array(
						SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
						SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
				),
				PROG_UPSAMPLE,
				4
			),

			PassConfig.of(
				"canvas:bloom_upsample_3",
				SamplerConfig.array(
						SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
						SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
				),
				PROG_UPSAMPLE,
				3
			),

			PassConfig.of(
				"canvas:bloom_upsample_2",
				SamplerConfig.array(
						SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
						SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
				),
				PROG_UPSAMPLE,
				2
			),

			PassConfig.of(
				"canvas:bloom_upsample_1",
				SamplerConfig.array(
						SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
						SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
				),
				PROG_UPSAMPLE,
				1
			),

			PassConfig.of(
				"canvas:bloom_upsample_0",
				SamplerConfig.array(
						SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
						SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
				),
				PROG_UPSAMPLE,
				0
			),

			PassConfig.of(
				"canvas:bloom",
				SamplerConfig.array(
						SamplerConfig.of(IMG_MAIN_COPY, false),
						SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
				),
				PROG_BLOOM,
				0
			)
		);
	}

	static final Identifier IMG_MC_MAIN = new Identifier("minecraft:main");
	static final Identifier IMG_EMISSIVE = new Identifier("canvas:emissive");
	static final Identifier IMG_EMISSIVE_COLOR = new Identifier("canvas:emissive_color");
	static final Identifier IMG_MAIN_COPY = new Identifier("canvas:main_copy");
	static final Identifier IMG_BLOOM_DOWNSAMPLE = new Identifier("canvas:bloom_downsample");
	static final Identifier IMG_BLOOM_UPSAMPLE = new Identifier("canvas:bloom_upsample");

	static final Identifier PROG_COPY = new Identifier("canvas:copy");
	static final Identifier PROG_EMISSIVE_COLOR = new Identifier("canvas:emissive_color");
	static final Identifier PROG_BLOOM = new Identifier("canvas:boom");
	static final Identifier PROG_COPY_LOD = new Identifier("canvas:copy_lod");
	static final Identifier PROG_DOWNSAMPLE = new Identifier("canvas:downsample");
	static final Identifier PROG_UPSAMPLE = new Identifier("canvas:upsample");
	static final Identifier PROG_UPSAMPLE_FIRST = new Identifier("canvas:upsample_first");
}
