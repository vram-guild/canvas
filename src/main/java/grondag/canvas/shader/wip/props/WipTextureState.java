/*
 * Copyright 2019, 2020 grondag
 *
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
 */

package grondag.canvas.shader.wip.props;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

public class WipTextureState {
	public static final int MAX_TEXTURE_STATES = 4096;
	private static int nextIndex = 1;
	private static final WipTextureState[] STATES = new WipTextureState[MAX_TEXTURE_STATES];
	private static final Object2ObjectOpenHashMap<Identifier, WipTextureState> MAP = new Object2ObjectOpenHashMap<>(256, Hash.VERY_FAST_LOAD_FACTOR);
	public static final WipTextureState NO_TEXTURE = new WipTextureState(0, null, null);

	static {
		STATES[0] = NO_TEXTURE;
		MAP.defaultReturnValue(NO_TEXTURE);
	}

	public static WipTextureState fromIndex(int index) {
		return STATES[index];
	}

	public static WipTextureState fromId(Identifier id) {
		return MAP.get(id);
	}

	public final int index;
	public final boolean isAtlas;
	public final Identifier id;
	public final AbstractTexture texture;

	private WipTextureState(int index, Identifier id, AbstractTexture texture) {
		this.index = index;
		this.id = id;
		this.texture  = texture;
		isAtlas = texture != null && texture instanceof SpriteAtlasTexture;
	}

	public static void onRegister(Identifier identifier, AbstractTexture texture) {
		final WipTextureState state = MAP.get(identifier);
		final int index = state == NO_TEXTURE ? nextIndex++ : state.index;
		final WipTextureState newState = new WipTextureState(index, identifier, texture);
		MAP.put(identifier, newState);
		STATES[index] = newState;
	}
}
