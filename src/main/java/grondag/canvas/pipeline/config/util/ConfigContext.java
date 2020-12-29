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

package grondag.canvas.pipeline.config.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.pipeline.config.PipelineParam;
import grondag.canvas.pipeline.config.ProgramConfig;

public class ConfigContext {
	public final Object2ObjectOpenHashMap<String, FramebufferConfig> frameBuffers = new Object2ObjectOpenHashMap<>();

	public final Object2ObjectOpenHashMap<String, ImageConfig> images = new Object2ObjectOpenHashMap<>();

	public final Object2ObjectOpenHashMap<String, PassConfig> passes = new Object2ObjectOpenHashMap<>();

	public final Object2ObjectOpenHashMap<String, PipelineParam> params = new Object2ObjectOpenHashMap<>();

	public final Object2ObjectOpenHashMap<String, ProgramConfig> programs = new Object2ObjectOpenHashMap<>();
}
