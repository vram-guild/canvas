/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
