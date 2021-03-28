/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.compat;

//import net.minecraft.client.render.Camera;
//import net.minecraft.client.render.Frustum;
//import net.minecraft.client.util.math.MatrixStack;
//
//import net.fabricmc.loader.api.FabricLoader;
//
//import grondag.canvas.CanvasMod;
//
//class SatinHolder {
//	static SatinOnWorldRendered onWorldRenderedEvent;
//	static SatinOnEntitiesRendered onEntitiesRenderedEvent;
//	static SatinBeforeEntitiesRendered beforeEntitiesRenderEvent;
//
//	static {
//		if (FabricLoader.getInstance().isModLoaded("satin")) {
//			CanvasMod.LOG.info("Found Satin - compatibility hook enabled");
//			onWorldRenderedEvent = SatinHelper.onWorldRenderedEvent();
//			onEntitiesRenderedEvent = SatinHelper.onEntitiesRenderedEvent();
//			beforeEntitiesRenderEvent = SatinHelper.beforeEntitiesRenderEvent();
//		} else {
//			onWorldRenderedEvent = (m, c, t, n) -> {
//			};
//			onEntitiesRenderedEvent = (c, f, t) -> {
//			};
//			beforeEntitiesRenderEvent = (c, f, t) -> {
//			};
//		}
//	}
//
//	@FunctionalInterface
//	interface SatinOnWorldRendered {
//		void onWorldRendered(MatrixStack matrices, Camera camera, float tickDelta, long nanoTime);
//	}
//
//	@FunctionalInterface
//	interface SatinOnEntitiesRendered {
//		void onEntitiesRendered(Camera camera, Frustum frustum, float tickDelta);
//	}
//
//	@FunctionalInterface
//	interface SatinBeforeEntitiesRendered {
//		void beforeEntitiesRender(Camera camera, Frustum frustum, float tickDelta);
//	}
//}
