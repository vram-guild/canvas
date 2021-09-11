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

package grondag.canvas.pipeline.pass;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineFramebuffer;
import grondag.canvas.pipeline.config.PassConfig;

public abstract class Pass {
	final PassConfig config;
	PipelineFramebuffer fbo;

	Pass(PassConfig config) {
		this.config = config;
		fbo = Pipeline.getFramebuffer(config.framebuffer.name);
	}

	public String getName() {
		return this.config.name;
	}

	public final boolean isEnabled() {
		return !config.toggleConfig.isValid() || config.toggleConfig.value().value();
	}

	public abstract void run(int width, int height);

	public abstract void close();

	public static Pass create(PassConfig config) {
		if (config.program.name.equals(PassConfig.CLEAR_NAME)) {
			return new ClearPass(config);
		} else {
			return new ProgramPass(config);
		}
	}

	static Pass[] create(String logName, PassConfig[] configs) {
		if (configs == null || configs.length == 0) {
			return new Pass[0];
		}

		final ObjectArrayList<Pass> passes = new ObjectArrayList<>();

		for (final PassConfig c : configs) {
			final Pass p = create(c);
			passes.add(p);
		}

		return passes.toArray(new Pass[passes.size()]);
	}
}
