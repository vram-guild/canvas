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
