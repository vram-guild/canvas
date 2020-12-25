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

import blue.endless.jankson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import grondag.canvas.Configurator;

public class PipelineConfig {
	public ImageConfig[] images;
	public PipelineParam[] params;
	public ShaderConfig[] shaders;
	public FramebufferConfig[] framebuffers;

	public PassConfig[] onWorldStart;
	public PassConfig[] afterRenderHand;

	{
		final Identifier id = new Identifier("canvas:pipeline/canvas_default.json");
		final ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
		JsonObject configJson = null;

		try (Resource res = rm.getResource(id)) {
			configJson = Configurator.JANKSON.load(res.getInputStream());
		} catch (final Exception e) {
			e.printStackTrace();
		}

		params = PipelineParam.array(
			PipelineParam.of("bloom_intensity", 0.0f, 0.5f, 0.1f),
			PipelineParam.of("bloom_scale", 0.0f, 2.0f, 0.25f)
		);

		images = ImageConfig.deserialize(configJson);
		shaders = ShaderConfig.deserialize(configJson);
		framebuffers = FramebufferConfig.deserialize(configJson);

		onWorldStart = PassConfig.array(
			PassConfig.of("emissive", new String[0], PassConfig.CLEAR_NAME, 0)
		);

		afterRenderHand = PassConfig.array(
			PassConfig.of(
				"main_copy",
				stringArray("default_main"),
				"copy",
				0
			),

			PassConfig.of(
				"emissive_color",
				stringArray("default_main", "emissive"),
				"emissive_color",
				0
			),

			PassConfig.of(
				"bloom_downsample_0",
				stringArray("emissive_color"),
				"downsample",
				0
			),

			PassConfig.of(
				"bloom_downsample_1",
				stringArray("bloom_downsample"),
				"downsample",
				1
			),

			PassConfig.of(
				"bloom_downsample_2",
				stringArray("bloom_downsample"),
				"downsample",
				2
			),

			PassConfig.of(
				"bloom_downsample_3",
				stringArray("bloom_downsample"),
				"downsample",
				3
			),

			PassConfig.of(
				"bloom_downsample_4",
				stringArray("bloom_downsample"),
				"downsample",
				4
			),

			PassConfig.of(
				"bloom_downsample_5",
				stringArray("bloom_downsample"),
				"downsample",
				5
			),

			PassConfig.of(
				"bloom_downsample_6",
				stringArray("bloom_downsample"),
				"downsample",
				6
			),

			PassConfig.of(
				"bloom_upsample_6",
				stringArray("bloom_downsample"),
				"upsample_first",
				6
			),

			PassConfig.of(
				"bloom_upsample_5",
				stringArray("bloom_downsample", "bloom_upsample"),
				"upsample",
				5
			),

			PassConfig.of(
				"bloom_upsample_4",
				stringArray("bloom_downsample", "bloom_upsample"),
				"upsample",
				4
			),

			PassConfig.of(
				"bloom_upsample_3",
				stringArray("bloom_downsample", "bloom_upsample"),
				"upsample",
				3
			),

			PassConfig.of(
				"bloom_upsample_2",
				stringArray("bloom_downsample", "bloom_upsample"),
				"upsample",
				2
			),

			PassConfig.of(
				"bloom_upsample_1",
				stringArray("bloom_downsample", "bloom_upsample"),
				"upsample",
				1
			),

			PassConfig.of(
				"bloom_upsample_0",
				stringArray("bloom_downsample", "bloom_upsample"),
				"upsample",
				0
			),

			PassConfig.of(
				"bloom",
				stringArray("main_copy", "bloom_upsample"),
				"bloom",
				0
			)
		);
	}

	String[] stringArray(String... strings) {
		return strings;
	}
}
