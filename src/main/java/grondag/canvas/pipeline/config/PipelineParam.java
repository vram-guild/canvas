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

public class PipelineParam {
	public String name;
	public float minVal;
	public float maxVal;
	public float defaultVal;
	public float currentVal;

	public static PipelineParam of(
		String name,
		float minVal,
		float maxVal,
		float defaultVal
	) {
		final PipelineParam result = new PipelineParam();
		result.name = name;
		result.minVal = minVal;
		result.maxVal = maxVal;
		result.defaultVal = defaultVal;
		result.currentVal = defaultVal;
		return result;
	}

	public static PipelineParam[] array(PipelineParam... params) {
		return params;
	}
}
