/*******************************************************************************
 * Copyright 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
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
