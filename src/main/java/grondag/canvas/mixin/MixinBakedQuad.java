package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;

import grondag.canvas.mixinext.BakedQuadExt;
import net.minecraft.client.render.model.BakedQuad;

/**
 * Canvas does shading in GPU, so we need to avoid modifying colors
 * on CPU and also indicate when diffuse should be disabled. This
 * handles the second problem.
 */
@Mixin(BakedQuad.class)
public abstract class MixinBakedQuad implements BakedQuadExt{
    private boolean disableDiffuse = false;

    @Override
    public boolean canvas_disableDiffuse() {
        return disableDiffuse;
    }

    @Override
    public void canvas_disableDiffuse(boolean disable) {
        disableDiffuse = disable;
    }
}
