package grondag.canvas.shader.wip;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

public class WipTextureState {
	public static final int MAX_TEXTURE_STATES = 4096;
	private static int nextIndex = 0;
	private static final WipTextureState[] STATES = new WipTextureState[MAX_TEXTURE_STATES];
	private static final Object2ObjectOpenHashMap<Identifier, WipTextureState> MAP = new Object2ObjectOpenHashMap<>(256, Hash.VERY_FAST_LOAD_FACTOR);

	public static WipTextureState fromIndex(int index) {
		return STATES[index];
	}

	public final int index;
	public final boolean isAtlas;
	public final Identifier id;
	public final AbstractTexture texture;

	private WipTextureState(int index, Identifier id, AbstractTexture texture) {
		this.index = index;
		this.id = id;
		this.texture  = texture;
		isAtlas = texture instanceof SpriteAtlasTexture;
	}

	public static void onRegister(Identifier identifier, AbstractTexture texture) {
		final WipTextureState state = MAP.get(identifier);
		final int index = state == null ? nextIndex++ : state.index;
		final WipTextureState newState = new WipTextureState(index, identifier, texture);
		MAP.put(identifier, newState);
		STATES[index] = newState;
	}
}
