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

package grondag.canvas.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.BufferBuilder;

public class BufferBuilderStore {
	private static final ObjectArrayList<BufferBuilder> builders = new ObjectArrayList<>();

	public static BufferBuilder claim() {
		assert RenderSystem.isOnRenderThread();

		if (builders.isEmpty()) {
			return new BufferBuilder(4096);
		} else {
			return builders.pop();
		}
	}

	/** Always returns null. */
	@Nullable
	public static BufferBuilder release(BufferBuilder builder) {
		assert RenderSystem.isOnRenderThread();
		assert builder != null;
		builders.push(builder);
		return null;
	}
}
