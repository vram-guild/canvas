package grondag.canvas.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import grondag.canvas.chunk.ChunkColorCache;
import grondag.canvas.mixinterface.WorldChunkExt;

@Mixin(WorldChunk.class)
public class MixinWorldChunk implements WorldChunkExt {
	private @Nullable ChunkColorCache colorCache;
	@Shadow private World world;

	@Override
	public ChunkColorCache canvas_colorCache() {
		ChunkColorCache result = colorCache;

		if (result == null || result.isInvalid()) {
			result = new ChunkColorCache((ClientWorld) world, (WorldChunk)(Object) this);
			colorCache = result;
		}

		return result;
	}

	@Override
	public void canvas_clearColorCache() {
		colorCache = null;
	}
}
