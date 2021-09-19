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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.pipeline.config.PipelineParam;
import grondag.canvas.pipeline.config.ProgramConfig;
import grondag.canvas.pipeline.config.option.BooleanConfigEntry;
import grondag.canvas.pipeline.config.option.EnumConfigEntry;
import grondag.canvas.pipeline.config.option.FloatConfigEntry;
import grondag.canvas.pipeline.config.option.IntConfigEntry;

public class ConfigContext {
	public final NamedDependencyMap<FramebufferConfig> frameBuffers = new NamedDependencyMap<>();

	// allow named in-game images
	public final NamedDependencyMap<ImageConfig> images = new NamedDependencyMap<>(s -> s.contains(":"));

	public final NamedDependencyMap<PassConfig> passes = new NamedDependencyMap<>();

	public final NamedDependencyMap<PipelineParam> params = new NamedDependencyMap<>();

	// allow built-in programs
	public final NamedDependencyMap<ProgramConfig> programs = new NamedDependencyMap<>(s -> s.equals(PassConfig.CLEAR_NAME));

	public final ObjectOpenHashSet<ResourceLocation> optionIds = new ObjectOpenHashSet<>();

	public final NamedDependencyMap<BooleanConfigEntry> booleanConfigEntries = new NamedDependencyMap<>();
	public final NamedDependencyMap<EnumConfigEntry> enumConfigEntries = new NamedDependencyMap<>();
	public final NamedDependencyMap<FloatConfigEntry> floatConfigEntries = new NamedDependencyMap<>();
	public final NamedDependencyMap<IntConfigEntry> intConfigEntries = new NamedDependencyMap<>();
}
