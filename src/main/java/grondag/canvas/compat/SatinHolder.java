/*
 * Copyright © Original Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
