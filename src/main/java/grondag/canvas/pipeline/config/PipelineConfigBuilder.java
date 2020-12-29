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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedDependency;

// WIP:  defaultFramebuffer target
// WIP: managed draw targets
public class PipelineConfigBuilder {
	public final ConfigContext context = new ConfigContext();
	public final ObjectArrayList<ImageConfig> images = new ObjectArrayList<>();
	public final ObjectArrayList<PipelineParam> params = new ObjectArrayList<>();
	public final ObjectArrayList<ProgramConfig> shaders = new ObjectArrayList<>();
	public final ObjectArrayList<FramebufferConfig> framebuffers = new ObjectArrayList<>();

	public final ObjectArrayList<PassConfig> onWorldStart = new ObjectArrayList<>();
	public final ObjectArrayList<PassConfig> afterRenderHand = new ObjectArrayList<>();
	public final ObjectArrayList<PassConfig> fabulous = new ObjectArrayList<>();

	@Nullable public FabulousConfig fabulosity;
	@Nullable public DrawTargetsConfig drawTargets;

	public NamedDependency<FramebufferConfig> defaultFramebuffer;

	public void load(JsonObject configJson) {
		params.add(PipelineParam.of(context, "bloom_intensity", 0.0f, 0.5f, 0.1f));
		params.add(PipelineParam.of(context, "bloom_scale", 0.0f, 2.0f, 0.25f));

		defaultFramebuffer = context.frameBuffers.createDependency(JanksonHelper.asString(configJson.get("defaultFramebuffer")));
		fabulosity = FabulousConfig.deserialize(context, configJson);
		drawTargets = DrawTargetsConfig.deserialize(context, configJson);

		PassConfig.deserialize(context, configJson, "fabulous", fabulous);
		PassConfig.deserialize(context, configJson, "onWorldRenderStart", onWorldStart);
		PassConfig.deserialize(context, configJson, "afterRenderHand", afterRenderHand);

		ImageConfig.deserialize(context, configJson, images);
		ProgramConfig.deserialize(context, configJson, shaders);
		FramebufferConfig.deserialize(context, configJson, framebuffers);
	}

	public boolean validate() {
		boolean valid = true;

		valid &= (drawTargets == null || drawTargets.validate());

		valid &= (fabulosity == null || fabulosity.validate());

		if (defaultFramebuffer == null || !defaultFramebuffer.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - missing or invalid defaultFramebuffer.");
			valid = false;
		}

		for (final FramebufferConfig fb : framebuffers) {
			valid &= fb.validate();
		}

		for (final ImageConfig img : images) {
			valid &= img.validate();
		}

		for (final PipelineParam param : params) {
			valid &= param.validate();
		}

		for (final ProgramConfig prog : shaders) {
			valid &= prog.validate();
		}

		return valid;
	}

	private static @Nullable PipelineConfigBuilder load(Identifier id) {
		final ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
		JsonObject configJson = null;

		final PipelineConfigBuilder result = new PipelineConfigBuilder();

		if (PipelineLoader.areResourcesAvailable() && rm != null) {
			try (Resource res = rm.getResource(id)) {
				configJson = Configurator.JANKSON.load(res.getInputStream());
				result.load(configJson);
			} catch (final Exception e) {
				// WIP: better logging
				e.printStackTrace();
			}
		} else {
			return null;
		}

		if (result.validate()) {
			return result;
		} else {
			// fallback to minimal renderable pipeline if not valid
			return null;
		}
	}

	public static PipelineConfig build(Identifier identifier) {
		final PipelineConfigBuilder builder = load(identifier);
		return builder == null ? PipelineConfig.minimalConfig() : new PipelineConfig(builder);
	}
}
