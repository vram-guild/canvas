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

import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class PipelineParam extends NamedConfig<PipelineParam> {
	protected PipelineParam(ConfigContext ctx, String name) {
		super(ctx, name);
	}

	public float minVal;
	public float maxVal;
	public float defaultVal;
	public float currentVal;

	public static PipelineParam of(
		ConfigContext ctx,
		String name,
		float minVal,
		float maxVal,
		float defaultVal
	) {
		final PipelineParam result = new PipelineParam(ctx, name);
		result.minVal = minVal;
		result.maxVal = maxVal;
		result.defaultVal = defaultVal;
		result.currentVal = defaultVal;
		return result;
	}

	public static PipelineParam[] array(PipelineParam... params) {
		return params;
	}

	@Override
	public boolean validate() {
		return super.validate();
	}

	@Override
	public NamedDependencyMap<PipelineParam> nameMap() {
		return context.params;
	}
}
