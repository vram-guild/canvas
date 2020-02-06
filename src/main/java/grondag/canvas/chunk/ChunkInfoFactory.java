/*******************************************************************************
 * Copyright 2019, 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.chunk;

import java.lang.reflect.Constructor;

import javax.annotation.Nullable;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.Direction;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.ChunkInfoExt;
import grondag.canvas.mixinterface.WorldRendererExt;

@FunctionalInterface
public interface ChunkInfoFactory {
	ChunkInfoExt get(WorldRendererExt parent, ChunkBuilder.BuiltChunk builtChunk, @Nullable Direction direction, int i);

	ChunkInfoFactory INSTANCE = Maker.make();

	public class Maker {
		private static ChunkInfoFactory make() {
			final String target = FabricLoader.getInstance().getMappingResolver()
					.mapClassName("intermediary", "net.minecraft.class_761$class_762");

			for (final Class<?> innerClass : WorldRenderer.class.getDeclaredClasses()) {
				if (innerClass.getName().equals(target)) {
					final Constructor<?> constructor = innerClass.getDeclaredConstructors()[0];
					constructor.setAccessible(true);

					return (p, bc, d, i) -> {
						try {
							final WorldRenderer wr = (WorldRenderer) p;
							final Direction face = d;
							return (ChunkInfoExt) constructor.newInstance(wr, bc, face, i);
						} catch (final Exception e) {
							CanvasMod.LOG.warn("[Canvas] Exception accessing ChunkInfo constructor", e);
							return null;
						}
					};
				}
			}

			CanvasMod.LOG.warn("[Canvas] Unable to access ChunkInfo constructor. Crash is probably imminent.");
			return (p, bc, d, i) -> null;
		}
	}
}
