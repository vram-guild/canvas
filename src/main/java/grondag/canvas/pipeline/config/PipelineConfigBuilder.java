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
import grondag.canvas.pipeline.config.util.LoadHelper;
import grondag.canvas.pipeline.config.util.NamedDependency;

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

		defaultFramebuffer = context.frameBuffers.dependOn(configJson, "defaultFramebuffer");
		fabulosity = LoadHelper.loadObject(context, configJson, "fabulousTargets", FabulousConfig::new);
		drawTargets = LoadHelper.loadObject(context, configJson, "drawTargets", DrawTargetsConfig::new);

		LoadHelper.loadSubList(context, configJson, "fabulous", "passes", fabulous, PassConfig::new);
		LoadHelper.loadSubList(context, configJson, "onWorldRenderStart", "passes", onWorldStart, PassConfig::new);
		LoadHelper.loadSubList(context, configJson, "afterRenderHand", "passes", afterRenderHand, PassConfig::new);

		LoadHelper.loadList(context, configJson, "images", images, ImageConfig::new);
		LoadHelper.loadList(context, configJson, "programs", shaders, ProgramConfig::new);
		LoadHelper.loadList(context, configJson, "framebuffers", framebuffers, FramebufferConfig::new);
	}

	public boolean validate() {
		boolean valid = true;

		if (drawTargets == null || !drawTargets.validate()) {
			CanvasMod.LOG.warn("Invalid pipeline config - missing or invalid drawTargets config.");
			valid = false;
		}

		valid &= (fabulosity == null || fabulosity.validate());

		valid &= defaultFramebuffer != null && defaultFramebuffer.validate("Invalid pipeline config - missing or invalid defaultFramebuffer.");

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
