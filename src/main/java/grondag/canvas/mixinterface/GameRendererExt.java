/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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

	int canvas_confusionAnimationTicks();
}
