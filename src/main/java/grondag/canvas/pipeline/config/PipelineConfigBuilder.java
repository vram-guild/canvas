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

import java.io.IOException;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
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

	public String materialVertexShader;
	public String materialFragmentShader;

	public void load(JsonObject configJson) {
		if (params.isEmpty()) {
			params.add(PipelineParam.of(context, "bloom_intensity", 0.0f, 0.5f, 0.1f));
			params.add(PipelineParam.of(context, "bloom_scale", 0.0f, 2.0f, 0.25f));
		}

		if (configJson.containsKey("defaultFramebuffer")) {
			// WIP: validate these are null when found - signals duplicate if not
			defaultFramebuffer = context.frameBuffers.dependOn(configJson, "defaultFramebuffer");
		}

		if (configJson.containsKey("fabulousTargets")) {
			fabulosity = LoadHelper.loadObject(context, configJson, "fabulousTargets", FabulousConfig::new);
		}

		if (configJson.containsKey("materialVertexShader")) {
			materialVertexShader = JanksonHelper.asString(configJson.get("materialVertexShader"));
		}

		if (configJson.containsKey("materialFragmentShader")) {
			materialFragmentShader = JanksonHelper.asString(configJson.get("materialFragmentShader"));
		}

		if (configJson.containsKey("drawTargets")) {
			drawTargets = LoadHelper.loadObject(context, configJson, "drawTargets", DrawTargetsConfig::new);
		}

		LoadHelper.loadSubList(context, configJson, "fabulous", "passes", fabulous, PassConfig::new);
		LoadHelper.loadSubList(context, configJson, "beforeWorldRender", "passes", onWorldStart, PassConfig::new);
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

		if (materialVertexShader == null || materialVertexShader.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - missing materialVertexShader.");
			valid = false;
		}

		if (materialFragmentShader == null || materialFragmentShader.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - missing materialFragmentShader.");
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

		if (!PipelineLoader.areResourcesAvailable() || rm == null) {
			return null;
		}

		final PipelineConfigBuilder result = new PipelineConfigBuilder();
		final ObjectOpenHashSet<Identifier> included = new ObjectOpenHashSet<>();
		final ObjectArrayFIFOQueue<Identifier> queue = new ObjectArrayFIFOQueue<>();

		queue.enqueue(id);
		included.add(id);

		while (!queue.isEmpty()) {
			final Identifier target = queue.dequeue();

			try (Resource res = rm.getResource(target)) {
				final JsonObject configJson = Configurator.JANKSON.load(res.getInputStream());
				result.load(configJson);
				getIncludes(configJson, included, queue);
			} catch (final IOException e) {
				CanvasMod.LOG.warn(String.format("Unable to load pipeline config resource %s due to IOExeption: %s", target.toString(), e.getLocalizedMessage()));
			} catch (final SyntaxError e) {
				CanvasMod.LOG.warn(String.format("Unable to load pipeline config resource %s due to Syntax Error: %s", target.toString(), e.getLocalizedMessage()));
			}
		}

		if (result.validate()) {
			return result;
		} else {
			// fallback to minimal renderable pipeline if not valid
			return null;
		}
	}

	private static void getIncludes(JsonObject configJson, ObjectOpenHashSet<Identifier> included, ObjectArrayFIFOQueue<Identifier> queue) {
		if (configJson == null || !configJson.containsKey("include")) {
			return;
		}

		final JsonArray array = JanksonHelper.getJsonArrayOrNull(configJson, "include", "Pipeline config error: 'include' must be an array.");
		final int limit = array.size();

		for (int i = 0; i < limit; ++i) {
			final String idString = JanksonHelper.asString(array.get(i));

			if (idString != null && !idString.isEmpty()) {
				final Identifier id = new Identifier(idString);

				if (included.add(id)) {
					queue.enqueue(id);
				}
			}
		}
	}

	public static PipelineConfig build(Identifier identifier) {
		final PipelineConfigBuilder builder = load(identifier);
		return builder == null ? PipelineConfig.minimalConfig() : new PipelineConfig(builder);
	}
}
