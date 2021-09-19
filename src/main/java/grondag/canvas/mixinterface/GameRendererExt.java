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

package grondag.canvas.mixinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

public interface GameRendererExt {
	float canvas_zoom();

	float canvas_zoomX();

	float canvas_zoomY();

	double canvas_getFov(Camera camera, float tickDelta, boolean changingFov);

	void canvas_bobViewWhenHurt(PoseStack matrixStack, float f);

	void canvas_bobView(PoseStack matrixStack, float f);

	int canvas_ticks();
}
