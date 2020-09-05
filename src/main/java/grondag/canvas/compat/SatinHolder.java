package grondag.canvas.compat;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class SatinHolder {
	static {
		if (FabricLoader.getInstance().isModLoaded("satin")) {
			CanvasMod.LOG.info("Found Satin - compatibility hook enabled");
			onWorldRenderedEvent = SatinHelper.onWorldRenderedEvent();
			onEntitiesRenderedEvent = SatinHelper.onEntitiesRenderedEvent();
			beforeEntitiesRenderEvent = SatinHelper.beforeEntitiesRenderEvent();
		} else {
			onWorldRenderedEvent = (m, c, t, n) -> {};
			onEntitiesRenderedEvent = (c, f, t) -> {};
			beforeEntitiesRenderEvent = (c, f, t) -> {};
		}
	}

	public static SatinOnWorldRendered onWorldRenderedEvent;
	public static SatinOnEntitiesRendered onEntitiesRenderedEvent;
	public static SatinBeforeEntitiesRendered beforeEntitiesRenderEvent;


	@FunctionalInterface
	public interface SatinOnWorldRendered {
		void onWorldRendered(MatrixStack matrices, Camera camera, float tickDelta, long nanoTime);
	}

	@FunctionalInterface
	public interface SatinOnEntitiesRendered {
		void onEntitiesRendered(Camera camera, Frustum frustum, float tickDelta);
	}

	@FunctionalInterface
	public interface SatinBeforeEntitiesRendered {
		void beforeEntitiesRender(Camera camera, Frustum frustum, float tickDelta);
	}
}
