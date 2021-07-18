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

package grondag.canvas.varia;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;

/**
 * Meant primarily for text rendering, creates an Immediate vertex consumer
 * that automatically adds render layers as needed to avoid intermediate draw calls.
 */
public class AutoImmediate {
	private AutoImmediate() { }

	public static final Immediate INSTANCE = create();

	private static Immediate create() {
		final Map<RenderLayer, BufferBuilder> map = new Object2ObjectOpenHashMap<>() {
			@Override
			public BufferBuilder getOrDefault(Object key, BufferBuilder defaultValue) {
				return get(key);
			}

			@Override
			public BufferBuilder get(Object key) {
				BufferBuilder result = super.get(key);

				if (result == null) {
					RenderLayer layer = (RenderLayer) key;
					result = new BufferBuilder(layer.getExpectedBufferSize());
					put(layer, result);
				}

				return result;
			}

			@Override
			public boolean containsKey(Object k) {
				return true;
			}
		};

		return VertexConsumerProvider.immediate(map, new BufferBuilder(0x1000));
	}
}
