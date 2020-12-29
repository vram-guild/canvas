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

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedConfig;

public class FramebufferConfig extends NamedConfig<FramebufferConfig> {
	public final AttachmentConfig[] colorAttachments;
	public final AttachmentConfig depthAttachment;
	public final boolean isValid;

	private FramebufferConfig(ConfigContext ctx, JsonObject config) {
		super(ctx, config.get(String.class, "name"));
		boolean valid = true;

		if (name == null || name.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - missing framebuffer name.");
			valid = false;
		}

		colorAttachments = AttachmentConfig.deserialize(ctx, config);

		for (final AttachmentConfig c : colorAttachments) {
			valid &= c.isValid;
		}

		if (config.containsKey("depthAttachment")) {
			depthAttachment = new AttachmentConfig(ctx, config.getObject("depthAttachment"), true);
			valid &= depthAttachment.isValid;
		} else {
			depthAttachment = null;
		}

		isValid = valid;
	}

	private FramebufferConfig(ConfigContext ctx) {
		super(ctx, "default");
		colorAttachments = new AttachmentConfig[] {AttachmentConfig.defaultMain(ctx)};
		depthAttachment = AttachmentConfig.defaultDepth(ctx);
		isValid = true;
	}

	public static FramebufferConfig[] deserialize(ConfigContext ctx, JsonObject configJson) {
		if (configJson == null || !configJson.containsKey("framebuffers")) {
			return new FramebufferConfig[0];
		}

		final JsonArray array = configJson.get(JsonArray.class, "framebuffers");
		final int limit = array.size();
		final FramebufferConfig[] result = new FramebufferConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new FramebufferConfig(ctx, (JsonObject) array.get(i));
		}

		return result;
	}

	public static FramebufferConfig makeDefault(ConfigContext context) {
		return new FramebufferConfig(context);
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object2ObjectOpenHashMap<String, FramebufferConfig> nameMap() {
		return context.frameBuffers;
	}
}
