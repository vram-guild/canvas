package grondag.canvas.compat;

import me.shedaniel.cloth.api.client.events.v0.ClothClientHooks;

public class ClothHelper {
	static Runnable clothDebugRenderPre()  {
		return () -> ClothClientHooks.DEBUG_RENDER_PRE.invoker().run();
	}
}
