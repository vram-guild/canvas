/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.pipeline.config;

import blue.endless.jankson.JsonObject;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class FramebufferConfig extends NamedConfig<FramebufferConfig> {
	public final AttachmentConfig[] colorAttachments;
	public final AttachmentConfig depthAttachment;

	FramebufferConfig(ConfigContext ctx, JsonObject config) {
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

	public static FramebufferConfig makeDefault(ConfigContext context) {
		return new FramebufferConfig(context);
	}

	@Override
	public boolean validate() {
		boolean valid = super.validate();

		for (final AttachmentConfig c : colorAttachments) {
			valid &= c.validate();

			if (c.isDepth) {
				CanvasMod.LOG.warn(String.format("Invalid pipeline config - depth attachment %s used as color attachment on framebuffer %s.",
						c.image.name, name));

				valid = false;
			}
		}

		if (depthAttachment != null) {
			valid &= depthAttachment.validate();

			if (!depthAttachment.isDepth) {
				CanvasMod.LOG.warn(String.format("Invalid pipeline config - color attachment %s used as depth attachment on framebuffer %s.",
						depthAttachment.image.name, name));

				valid = false;
			}
		}

		return valid;
	}

	@Override
	public NamedDependencyMap<FramebufferConfig> nameMap() {
		return context.frameBuffers;
	}
}
