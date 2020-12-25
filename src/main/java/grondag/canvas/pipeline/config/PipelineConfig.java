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
	public ProgramConfig[] shaders;
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
		shaders = ProgramConfig.deserialize(configJson);
		framebuffers = FramebufferConfig.deserialize(configJson);
		onWorldStart = PassConfig.deserialize(configJson, "onWorldRenderStart");
		afterRenderHand = PassConfig.deserialize(configJson, "afterRenderHand");
	}

	String[] stringArray(String... strings) {
		return strings;
	}
}
