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

import grondag.canvas.CanvasMod;

public class FramebufferConfig {
	public final String name;
	public final AttachmentConfig[] colorAttachments;
	public final AttachmentConfig depthAttachment;
	public final boolean isValid;

	private FramebufferConfig(JsonObject config) {
		boolean valid = true;

		String name = config.get(String.class, "name");

		if (name == null || name.isEmpty()) {
			name = "missing";
			CanvasMod.LOG.warn("Invalid pipeline config - missing framebuffer name.");
			valid = false;
		}

		this.name = name;

		colorAttachments = AttachmentConfig.deserialize(config);

		for (final AttachmentConfig c : colorAttachments) {
			valid &= c.isValid;
		}

		if (config.containsKey("depthAttachment")) {
			depthAttachment = new AttachmentConfig(config.getObject("depthAttachment"), true);
			valid &= depthAttachment.isValid;
		} else {
			depthAttachment = null;
		}

		isValid = valid;
	}

	private FramebufferConfig() {
		name = "default";
		colorAttachments = new AttachmentConfig[] {AttachmentConfig.DEFAULT_MAIN};
		depthAttachment = AttachmentConfig.DEFAULT_DEPTH;
		isValid = true;
	}

	public static FramebufferConfig[] deserialize(JsonObject configJson) {
		if (configJson == null || !configJson.containsKey("framebuffers")) {
			return new FramebufferConfig[0];
		}

		final JsonArray array = configJson.get(JsonArray.class, "framebuffers");
		final int limit = array.size();
		final FramebufferConfig[] result = new FramebufferConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new FramebufferConfig((JsonObject) array.get(i));
		}

		return result;
	}

	public static final FramebufferConfig DEFAULT = new FramebufferConfig();
}
