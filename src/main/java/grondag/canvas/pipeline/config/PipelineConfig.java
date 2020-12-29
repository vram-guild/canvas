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
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;

// WIP:  defaultFramebuffer target
// WIP: managed draw targets
public class PipelineConfig {
	public final ConfigContext context = new ConfigContext();
	public final ImageConfig[] images;
	public final PipelineParam[] params;
	public final ProgramConfig[] shaders;
	public final FramebufferConfig[] framebuffers;

	public final PassConfig[] onWorldStart;
	public final PassConfig[] afterRenderHand;
	public final PassConfig[] fabulous;

	@Nullable public final FabulousConfig fabulosity;
	@Nullable public final DrawTargetsConfig drawTargets;

	public final boolean isValid;

	public final String defaultFramebuffer;

	private PipelineConfig() {
		params = new PipelineParam[0];
		shaders = new ProgramConfig[0];
		onWorldStart = new PassConfig[0];
		afterRenderHand = new PassConfig[0];
		fabulous = new PassConfig[0];
		images = new ImageConfig[] { ImageConfig.defaultMain(context), ImageConfig.defaultDepth(context) };
		framebuffers = new FramebufferConfig[] { FramebufferConfig.makeDefault(context) };
		fabulosity = null;
		drawTargets = DrawTargetsConfig.makeDefault(context);
		defaultFramebuffer = "default";
		isValid = true;
	}

	private PipelineConfig (JsonObject configJson) {
		boolean valid = true;
		params = PipelineParam.array(
			PipelineParam.of(context, "bloom_intensity", 0.0f, 0.5f, 0.1f),
			PipelineParam.of(context, "bloom_scale", 0.0f, 2.0f, 0.25f)
		);

		defaultFramebuffer = JanksonHelper.asString(configJson.get("defaultFramebuffer"));

		if (defaultFramebuffer == null || defaultFramebuffer.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - missing defaultFramebuffer.");
			valid = false;
		}

		fabulosity = FabulousConfig.deserialize(context, configJson);
		fabulous = PassConfig.deserialize(context, configJson, "fabulous");
		valid &= (fabulosity == null || fabulosity.isValid);

		drawTargets = DrawTargetsConfig.deserialize(context, configJson);
		valid &= (drawTargets == null || drawTargets.isValid);

		images = ImageConfig.deserialize(context, configJson);
		shaders = ProgramConfig.deserialize(context, configJson);
		framebuffers = FramebufferConfig.deserialize(context, configJson);
		onWorldStart = PassConfig.deserialize(context, configJson, "onWorldRenderStart");
		afterRenderHand = PassConfig.deserialize(context, configJson, "afterRenderHand");

		for (final FramebufferConfig fb : framebuffers) {
			valid &= fb.isValid;
		}

		isValid = valid;
	}

	public static @Nullable PipelineConfig load(Identifier id) {
		final ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
		JsonObject configJson = null;

		PipelineConfig result = null;

		if (PipelineLoader.areResourcesAvailable() && rm != null) {
			try (Resource res = rm.getResource(id)) {
				configJson = Configurator.JANKSON.load(res.getInputStream());
				result = new PipelineConfig(configJson);
			} catch (final Exception e) {
				// WIP: better logging
				e.printStackTrace();
			}
		}

		if (result != null && result.isValid) {
			return result;
		} else {
			// fallback to minimal renderable pipeline if not valid
			return new PipelineConfig();
		}
	}

	public static final Identifier DEFAULT_ID = new Identifier("canvas:pipelines/canvas_default.json");
}
