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
