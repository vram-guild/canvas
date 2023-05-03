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

package grondag.canvas.pipeline.pass;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineFramebuffer;
import grondag.canvas.pipeline.config.PassConfig;

public abstract class Pass {
	final PassConfig config;
	PipelineFramebuffer fbo;

	Pass(PassConfig config) {
		this.config = config;
		loadFramebuffer();
	}

	public String getName() {
		return this.config.name;
	}

	public final boolean isEnabled() {
		return !config.toggleConfig.isValid() || config.toggleConfig.value().value();
	}

	public abstract void run();

	public void loadFramebuffer() {
		this.fbo = Pipeline.getFramebuffer(config.framebuffer);
	}

	public abstract void close();

	public static Pass create(PassConfig config) {
		if (config.program.name.equals(PassConfig.CLEAR_NAME)) {
			return new ClearPass(config);
		} else {
			return new ProgramPass(config);
		}
	}
}
