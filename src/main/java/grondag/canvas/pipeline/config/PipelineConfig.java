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

package grondag.canvas.pipeline.config;

import org.lwjgl.opengl.GL21;

public class PipelineConfig {
	public ImageConfig[] images;
	public PipelineParam[] params;
	public ShaderConfig[] shaders;
	public FramebufferConfig[] framebuffers;

	public PassConfig[] onWorldStart;
	public PassConfig[] afterRenderHand;

	{
		params = PipelineParam.array(
			PipelineParam.of("bloom_intensity", 0.0f, 0.5f, 0.1f),
			PipelineParam.of("bloom_scale", 0.0f, 2.0f, 0.25f)
		);

		images = ImageConfig.array(
			ImageConfig.of("emissive", false, GL21.GL_RGBA8, GL21.GL_LINEAR, GL21.GL_LINEAR, 0),
			ImageConfig.of("emissive_color", false, GL21.GL_RGBA8, GL21.GL_LINEAR, GL21.GL_LINEAR, 0),
			// don't want filtering when copy back from main
			ImageConfig.of("main_copy", false, GL21.GL_RGBA8, GL21.GL_NEAREST, GL21.GL_NEAREST, 0),
			ImageConfig.of("bloom_downsample", false, GL21.GL_RGBA8, GL21.GL_LINEAR_MIPMAP_NEAREST, GL21.GL_LINEAR, 6),
			ImageConfig.of("bloom_upsample", false, GL21.GL_RGBA8, GL21.GL_LINEAR_MIPMAP_NEAREST, GL21.GL_LINEAR, 6)
		);

		shaders = ShaderConfig.array(
			ShaderConfig.of("copy", "canvas:shaders/internal/process/copy.vert", "canvas:shaders/internal/process/copy.frag", "_cvu_input", "tex0"),

			ShaderConfig.of(
				"emissive_color",
				"canvas:shaders/internal/process/emissive_color.vert",
				"canvas:shaders/internal/process/emissive_color.frag",
				"_cvu_base", "_cvu_emissive"
			),

			ShaderConfig.of(
				"boom",
				"canvas:shaders/internal/process/bloom.vert",
				"canvas:shaders/internal/process/bloom.frag",
				"_cvu_base", "_cvu_bloom"
			),

			ShaderConfig.of(
				"copy_lod",
				"canvas:shaders/internal/process/copy_lod.vert",
				"canvas:shaders/internal/process/copy_lod.frag",
				"_cvu_input"
			),

			ShaderConfig.of(
				"downsample",
				"canvas:shaders/internal/process/downsample.vert",
				"canvas:shaders/internal/process/downsample.frag",
				"_cvu_input"
			),

			ShaderConfig.of(
				"upsample",
				"canvas:shaders/internal/process/upsample.vert",
				"canvas:shaders/internal/process/upsample.frag",
				"_cvu_input", "cvu_prior"
			),

			ShaderConfig.of(
					"upsample_first",
				"canvas:shaders/internal/process/upsample.vert",
				"canvas:shaders/internal/process/upsample_first.frag",
				"_cvu_input"
			)
		);

		framebuffers = FramebufferConfig.array(
			FramebufferConfig.of(
				"emissive",
				AttachmentConfig.array(AttachmentConfig.of("emissive", 0, 0))
			),

			FramebufferConfig.of(
				"main_copy",
				AttachmentConfig.array(AttachmentConfig.of("main_copy", 0, 0))
			),

			FramebufferConfig.of(
				"emissive_color",
				AttachmentConfig.array(AttachmentConfig.of("emissive_color", 0, 0))
			),

			FramebufferConfig.of(
				"bloom_downsample_0",
				AttachmentConfig.array(AttachmentConfig.of("bloom_downsample", 0, 0))
			),

			FramebufferConfig.of(
				"bloom_downsample_1",
				AttachmentConfig.array(AttachmentConfig.of("bloom_downsample", 0, 1))
			),

			FramebufferConfig.of(
				"bloom_downsample_2",
				AttachmentConfig.array(AttachmentConfig.of("bloom_downsample", 0, 2))
			),

			FramebufferConfig.of(
				"bloom_downsample_3",
				AttachmentConfig.array(AttachmentConfig.of("bloom_downsample", 0, 3))
			),

			FramebufferConfig.of(
				"bloom_downsample_4",
				AttachmentConfig.array(AttachmentConfig.of("bloom_downsample", 0, 4))
			),

			FramebufferConfig.of(
				"bloom_downsample_5",
				AttachmentConfig.array(AttachmentConfig.of("bloom_downsample", 0, 5))
			),

			FramebufferConfig.of(
				"bloom_downsample_6",
				AttachmentConfig.array(AttachmentConfig.of("bloom_downsample", 0, 6))
			),

			FramebufferConfig.of(
				"bloom_upsample_6",
				AttachmentConfig.array(AttachmentConfig.of("bloom_upsample", 0, 6))
			),

			FramebufferConfig.of(
				"bloom_upsample_5",
				AttachmentConfig.array(AttachmentConfig.of("bloom_upsample", 0, 5))
			),

			FramebufferConfig.of(
				"bloom_upsample_4",
				AttachmentConfig.array(AttachmentConfig.of("bloom_upsample", 0, 4))
			),

			FramebufferConfig.of(
				"bloom_upsample_3",
				AttachmentConfig.array(AttachmentConfig.of("bloom_upsample", 0, 3))
			),

			FramebufferConfig.of(
				"bloom_upsample_2",
				AttachmentConfig.array(AttachmentConfig.of("bloom_upsample", 0, 2))
			),

			FramebufferConfig.of(
				"bloom_upsample_1",
				AttachmentConfig.array(AttachmentConfig.of("bloom_upsample", 0, 1))
			),

			FramebufferConfig.of(
				"bloom_upsample_0",
				AttachmentConfig.array(AttachmentConfig.of("bloom_upsample", 0, 0))
			),

			FramebufferConfig.of(
				"bloom",
				AttachmentConfig.array(AttachmentConfig.of("default_main", 0, 0))
			)
		);

		onWorldStart = PassConfig.array(
			PassConfig.of("emissive", SamplerConfig.array(), PassConfig.CLEAR_NAME, 0)
		);

		afterRenderHand = PassConfig.array(
			PassConfig.of(
				"main_copy",
				SamplerConfig.array(SamplerConfig.of("default_main", false)),
				"copy",
				0
			),

			PassConfig.of(
				"emissive_color",
				SamplerConfig.array(
						SamplerConfig.of("default_main", false),
						SamplerConfig.of("emissive", false)
				),
				"emissive_color",
				0
			),

			PassConfig.of(
				"bloom_downsample_0",
				SamplerConfig.array(SamplerConfig.of("emissive_color", false)),
				"downsample",
				0
			),

			PassConfig.of(
				"bloom_downsample_1",
				SamplerConfig.array(SamplerConfig.of("bloom_downsample", false)),
				"downsample",
				1
			),

			PassConfig.of(
				"bloom_downsample_2",
				SamplerConfig.array(SamplerConfig.of("bloom_downsample", false)),
				"downsample",
				2
			),

			PassConfig.of(
				"bloom_downsample_3",
				SamplerConfig.array(SamplerConfig.of("bloom_downsample", false)),
				"downsample",
				3
			),

			PassConfig.of(
				"bloom_downsample_4",
				SamplerConfig.array(SamplerConfig.of("bloom_downsample", false)),
				"downsample",
				4
			),

			PassConfig.of(
				"bloom_downsample_5",
				SamplerConfig.array(SamplerConfig.of("bloom_downsample", false)),
				"downsample",
				5
			),

			PassConfig.of(
				"bloom_downsample_6",
				SamplerConfig.array(SamplerConfig.of("bloom_downsample", false)),
				"downsample",
				6
			),

			PassConfig.of(
				"bloom_upsample_6",
				SamplerConfig.array(
						SamplerConfig.of("bloom_downsample", false)
				),
				"upsample_first",
				6
			),

			PassConfig.of(
				"bloom_upsample_5",
				SamplerConfig.array(
						SamplerConfig.of("bloom_downsample", false),
						SamplerConfig.of("bloom_upsample", false)
				),
				"upsample",
				5
			),

			PassConfig.of(
				"bloom_upsample_4",
				SamplerConfig.array(
						SamplerConfig.of("bloom_downsample", false),
						SamplerConfig.of("bloom_upsample", false)
				),
				"upsample",
				4
			),

			PassConfig.of(
				"bloom_upsample_3",
				SamplerConfig.array(
						SamplerConfig.of("bloom_downsample", false),
						SamplerConfig.of("bloom_upsample", false)
				),
				"upsample",
				3
			),

			PassConfig.of(
				"bloom_upsample_2",
				SamplerConfig.array(
						SamplerConfig.of("bloom_downsample", false),
						SamplerConfig.of("bloom_upsample", false)
				),
				"upsample",
				2
			),

			PassConfig.of(
				"bloom_upsample_1",
				SamplerConfig.array(
						SamplerConfig.of("bloom_downsample", false),
						SamplerConfig.of("bloom_upsample", false)
				),
				"upsample",
				1
			),

			PassConfig.of(
				"bloom_upsample_0",
				SamplerConfig.array(
						SamplerConfig.of("bloom_downsample", false),
						SamplerConfig.of("bloom_upsample", false)
				),
				"upsample",
				0
			),

			PassConfig.of(
				"bloom",
				SamplerConfig.array(
						SamplerConfig.of("main_copy", false),
						SamplerConfig.of("bloom_upsample", false)
				),
				"boom",
				0
			)
		);
	}
}
