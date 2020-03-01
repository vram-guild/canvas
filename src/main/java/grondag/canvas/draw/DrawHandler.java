package grondag.canvas.draw;

import net.minecraft.client.render.RenderLayer;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.material.MaterialVertexFormat;

public abstract class DrawHandler {
	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	public final MaterialShaderImpl shader;
	public final MaterialConditionImpl condition;
	public final MaterialVertexFormat format;

	public RenderLayer renderLayer;

	DrawHandler (MaterialVertexFormat format, Value mat) {
		this.format = format;

		// TODO: egregious hack is egregious
		RenderLayer renderLayer = RenderLayer.getSolid();

		if (mat.blendMode(0) == BlendMode.CUTOUT) {
			renderLayer = RenderLayer.getCutout();
		} else if (mat.blendMode(0) == BlendMode.CUTOUT_MIPPED) {
			renderLayer = RenderLayer.getCutoutMipped();
		} else if (mat.blendMode(0) == BlendMode.TRANSLUCENT) {
			renderLayer = RenderLayer.getTranslucent();
		}

		this.renderLayer = renderLayer;
		shader = mat.shader;
		condition = mat.condition;
	}

	protected abstract void activate();

	public void draw(DrawableDelegate delegate) {
		//state.activate(OldShaderContext.BLOCK_TRANSLUCENT);
		activate();
		delegate.bind();
		delegate.draw();
	}
}
