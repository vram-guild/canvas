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

import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;

import net.fabricmc.fabric.impl.client.indigo.Indigo;
import net.fabricmc.loader.api.FabricLoader;

@FunctionalInterface
public interface RebuildTaskFactory {
	Object get(BuiltChunk parent, double dist);

	RebuildTaskFactory INSTANCE = Maker.make();

	public class Maker {
		private static RebuildTaskFactory make() {
			final String target = FabricLoader.getInstance().getMappingResolver()
					.mapClassName("intermediary", "net.minecraft.class_846$class_851$class_4578");

			for (final Class<?> innerClass : BuiltChunk.class.getDeclaredClasses()) {
				if (innerClass.getName().equals(target)) {
					final Constructor<?> constructor = innerClass.getDeclaredConstructors()[0];
					constructor.setAccessible(true);

					return (p, d) -> {
						try {
							return constructor.newInstance(p, d, null);
						} catch (final Exception e) {
							Indigo.LOGGER.warn("[Canvas] Exception accessing RebuildTask constructor", e);
							return null;
						}
					};
				}
			}

			return (p, d) -> null;
		}
	}
}

