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
		int attachmentIndex;
		int lod;

		static AttachmentConfig of(
			Identifier image,
			int attachmentIndex,
			int lod
		) {
			final AttachmentConfig result = new AttachmentConfig();
			result.image = image;
			result.attachmentIndex = attachmentIndex;
			result.lod = lod;
			return result;
		}

		static AttachmentConfig of(
			String image,
			int attachmentIndex,
			int lod
		) {
			return of(new Identifier(image), attachmentIndex, lod);
		}

		static AttachmentConfig[] array(AttachmentConfig... attachments) {
			return attachments;
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
		AttachmentConfig[] attachments;
		SamplerConfig[] samplers;
		Identifier shader;
		// for computing size
		int lod;

		//Image[] attachments, int[] samplerBinds, ProcessShader shader, Consumer<ProcessShader> activator

		static PassConfig of(
			AttachmentConfig[] attachments,
			SamplerConfig[] samplers,
			Identifier shader,
			int lod
		) {
			final PassConfig result = new PassConfig();
			result.attachments = attachments;
			result.samplers = samplers;
			result.shader = shader;
			result.lod = lod;
			return result;
		}

		static PassConfig[] array(PassConfig... passes) {
			return passes;
		}
	}

	static class ClearConfig {
		Identifier image;
		int clearColor;

		static ClearConfig of(
			Identifier image,
			int clearColor
		) {
			final ClearConfig result = new ClearConfig();
			result.image = image;
			result.clearColor = clearColor;
			return result;
		}

		static ClearConfig[] array(ClearConfig... configs) {
			return configs;
		}
	}

	static class StageConfig {
		ClearConfig[] clears;
		PassConfig[] passes;

		static StageConfig of(
			ClearConfig[] clears,
			PassConfig[] passes
		) {
			final StageConfig result = new StageConfig();
			result.clears = clears;
			result.passes = passes;
			return result;
		}
	}

	ImageConfig[] images;
	PipelineParam[] params;
	ShaderConfig[] shaders;

	StageConfig onWorldStart;
	StageConfig afterRenderHand;

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

		onWorldStart = StageConfig.of(
			ClearConfig.array(ClearConfig.of(IMG_EMISSIVE, 0)),
			new PassConfig[0]
		);

		afterRenderHand = StageConfig.of(
			new ClearConfig[0],
			PassConfig.array(
				// Bloom based on approach described by Jorge Jiminez, 2014
				// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare

				// copy MC fbo color attachment - need it at end for combine step
				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_MAIN_COPY, 0, 0)),
					SamplerConfig.array(SamplerConfig.of(IMG_MC_MAIN, false)),
					PROG_COPY,
					0
				),

				// select emissive portions for blur
				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_EMISSIVE_COLOR, 0, 0)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_MC_MAIN, false),
							SamplerConfig.of(IMG_EMISSIVE, false)
					),
					PROG_EMISSIVE_COLOR,
					0
				),

				// build bloom mipmaps, blurring as part of downscale

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 0)),
					SamplerConfig.array(SamplerConfig.of(IMG_EMISSIVE_COLOR, false)),
					PROG_DOWNSAMPLE,
					0
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 1)),
					SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
					PROG_DOWNSAMPLE,
					1
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 2)),
					SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
					PROG_DOWNSAMPLE,
					2
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 3)),
					SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
					PROG_DOWNSAMPLE,
					3
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 4)),
					SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
					PROG_DOWNSAMPLE,
					4
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 5)),
					SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
					PROG_DOWNSAMPLE,
					5
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_DOWNSAMPLE, 0, 6)),
					SamplerConfig.array(SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)),
					PROG_DOWNSAMPLE,
					6
				),

				// upscale bloom mipmaps, bluring again as we go

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 6)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false)
					),
					PROG_UPSAMPLE_FIRST,
					6
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 5)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
							SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
					),
					PROG_UPSAMPLE,
					5
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 4)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
							SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
					),
					PROG_UPSAMPLE,
					4
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 3)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
							SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
					),
					PROG_UPSAMPLE,
					3
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 2)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
							SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
					),
					PROG_UPSAMPLE,
					2
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 1)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
							SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
					),
					PROG_UPSAMPLE,
					1
				),

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_BLOOM_UPSAMPLE, 0, 0)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_BLOOM_DOWNSAMPLE, false),
							SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
					),
					PROG_UPSAMPLE,
					0
				),

				// Switch back to MC main color to draw combined color + bloom
				// Framebuffer attachment shouldn't draw to self so use copy created earlier

				PassConfig.of(
					AttachmentConfig.array(AttachmentConfig.of(IMG_MC_MAIN, 0, 0)),
					SamplerConfig.array(
							SamplerConfig.of(IMG_MAIN_COPY, false),
							SamplerConfig.of(IMG_BLOOM_UPSAMPLE, false)
					),
					PROG_BLOOM,
					0
				)
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
