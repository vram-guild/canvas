package grondag.canvas.compat;

import ladysnake.satin.api.event.EntitiesPostRenderCallback;
import ladysnake.satin.api.event.EntitiesPreRenderCallback;
import ladysnake.satin.api.event.PostWorldRenderCallbackV2;

import grondag.canvas.compat.SatinHolder.SatinBeforeEntitiesRendered;
import grondag.canvas.compat.SatinHolder.SatinOnEntitiesRendered;
import grondag.canvas.compat.SatinHolder.SatinOnWorldRendered;

public class SatinHelper {
	static SatinOnWorldRendered onWorldRenderedEvent() {
		return PostWorldRenderCallbackV2.EVENT.invoker()::onWorldRendered;
	}

	static SatinOnEntitiesRendered onEntitiesRenderedEvent() {
		return EntitiesPostRenderCallback.EVENT.invoker()::onEntitiesRendered;
	}

	static SatinBeforeEntitiesRendered beforeEntitiesRenderEvent() {
		return EntitiesPreRenderCallback.EVENT.invoker()::beforeEntitiesRender;
	}
}
