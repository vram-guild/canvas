package grondag.canvas.mixin;

import com.google.common.collect.ImmutableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase;

@Mixin(MultiPhaseParameters.class)
public interface AccessMultiPhaseParameters {
	@Accessor RenderPhase.Texture getTexture();
	@Accessor RenderPhase.Transparency getTransparency();
	@Accessor RenderPhase.DiffuseLighting getDiffuseLighting();
	@Accessor RenderPhase.ShadeModel getShadeModel();
	@Accessor RenderPhase.Alpha getAlpha();
	@Accessor RenderPhase.DepthTest getDepthTest();
	@Accessor RenderPhase.Cull getCull();
	@Accessor RenderPhase.Lightmap getLightmap();
	@Accessor RenderPhase.Overlay getOverlay();
	@Accessor RenderPhase.Fog getFog();
	@Accessor RenderPhase.Layering getLayering();
	@Accessor RenderPhase.Target getTarget();
	@Accessor RenderPhase.Texturing getTexturing();
	@Accessor RenderPhase.WriteMaskState getWriteMaskState();
	@Accessor RenderPhase.LineWidth getLineWidth();
	@Accessor RenderLayer.OutlineMode getOutlineMode();
	@Accessor ImmutableList<RenderPhase> getPhases();
}
