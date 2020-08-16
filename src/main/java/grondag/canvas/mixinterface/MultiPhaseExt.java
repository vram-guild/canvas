package grondag.canvas.mixinterface;

import java.util.Optional;

import net.minecraft.client.render.RenderLayer;

import grondag.canvas.mixin.AccessMultiPhaseParameters;

public interface MultiPhaseExt {

	Optional<RenderLayer> canvas_affectedOutline();

	boolean canvas_outline();

	void canvas_startDrawing();

	void canvas_endDrawing();

	AccessMultiPhaseParameters canvas_phases();
}
