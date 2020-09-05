package grondag.canvas.compat;

import com.google.common.util.concurrent.Runnables;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class ClothHolder {
	public static Runnable clothDebugPreEvent = init(); //FabricLoader.getInstance().isModLoaded("cloth-client-events-v0") ? ClothHelper.clothDebugRenderPre() : Runnables.doNothing();

	static Runnable init() {
		if (FabricLoader.getInstance().isModLoaded("cloth-client-events-v0")) {
			CanvasMod.LOG.info("Found Cloth Client Events - compatibility hook enabled");
			return ClothHelper.clothDebugRenderPre();
		} else {
			return Runnables.doNothing();
		}
	}
}
