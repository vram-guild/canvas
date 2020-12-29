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

import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class FramebufferConfig extends NamedConfig<FramebufferConfig> {
	public final AttachmentConfig[] colorAttachments;
	public final AttachmentConfig depthAttachment;

	private FramebufferConfig(ConfigContext ctx, JsonObject config) {
		super(ctx, config.get(String.class, "name"));

		colorAttachments = AttachmentConfig.deserialize(ctx, config);

		if (config.containsKey("depthAttachment")) {
			depthAttachment = new AttachmentConfig(ctx, config.getObject("depthAttachment"), true);
		} else {
			depthAttachment = null;
		}
	}

	private FramebufferConfig(ConfigContext ctx) {
		super(ctx, "default");
		colorAttachments = new AttachmentConfig[] {AttachmentConfig.defaultMain(ctx)};
		depthAttachment = AttachmentConfig.defaultDepth(ctx);
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
	public boolean validate() {
		boolean valid = super.validate();

		for (final AttachmentConfig c : colorAttachments) {
			valid &= c.validate();
		}

		if (depthAttachment != null) {
			valid &= depthAttachment.validate();
		}

		return valid;
	}

	@Override
	public NamedDependencyMap<FramebufferConfig> nameMap() {
		return context.frameBuffers;
	}
}
