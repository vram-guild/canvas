package grondag.canvas.terrain;

import java.util.concurrent.atomic.AtomicReference;

class RegionBuildState {
	/**
	 * Set by main thread during schedule. Retrieved and set to null by worker
	 * right before building.
	 *
	 * Special values also signal the need for translucency sort and chunk reset.
	 */
	final AtomicReference<ProtoRenderRegion> protoRegion = new AtomicReference<>(ProtoRenderRegion.IDLE);
}