/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
package grondag.canvas.light;

import com.mojang.blaze3d.platform.GlStateManager;


public abstract class GuiLightingHelper {
	private static boolean enabled = false;

	public static void suspend() {
		if(enabled) {
			GlStateManager.disableLighting();
			//GlStateManager.disableLight(0);
			//GlStateManager.disableLight(1);
			GlStateManager.disableColorMaterial();
		}
	}

	public static void resume() {
		if(enabled) {
			GlStateManager.enableLighting();
			//GlStateManager.enableLight(0);
			//GlStateManager.enableLight(1);
			GlStateManager.enableColorMaterial();
		}
	}

	public static void notifyStatus(boolean enabledIn) {
		enabled = enabledIn;
	}
}
