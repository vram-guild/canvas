package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.render.chunk.ChunkBuilder;

@Mixin(ChunkBuilder.class)
public abstract class MixinChunkBuilder {
	@ModifyVariable(method = "<init>", index = 9, at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayListWithExpectedSize(I)Ljava/util/ArrayList;", remap = false))
	private int onInitZeroListSize(int ignored) {
		return 0;
	}

}
