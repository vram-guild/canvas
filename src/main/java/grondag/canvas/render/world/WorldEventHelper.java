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

package grondag.canvas.render.world;

import com.mojang.blaze3d.systems.RenderSystem;

import io.vram.frex.api.renderloop.EntityRenderPostListener;
import io.vram.frex.api.renderloop.EntityRenderPreListener;
import io.vram.frex.api.renderloop.TranslucentPostListener;
import io.vram.frex.api.renderloop.WorldRenderContext;

/**
 * Matrix insanity.
 *
 * Canvas always applies view matrix on shader regardless of terrain or entity render.
 * Therefore, all renders typically uses identity pose stack, and it works fine with Blaze3D
 * renders due to having internal view matrix that can be set by us. However, third party renders
 * that bypasses both Canvas and Blaze3D renders (i.e. uses their own shader program/matrix state
 * or GL draw calls) will fail without the appropriate compatibility.
 *
 * Note that this class is NOT public API.
 */
public class WorldEventHelper {

	/**
	 * When true, pose stack pose matrix is identity, while RenderSystem has the correct view matrix.
	 */
	public static boolean poseIsIdentity;

	static void startIdentity(WorldRenderContext ctx) {
		ctx.poseStack().last().pose().setIdentity();
		poseIsIdentity = true;
	}

	static void endIdentity(WorldRenderContext ctx) {
		ctx.poseStack().last().pose().load(RenderSystem.getModelViewMatrix());
		poseIsIdentity = false;
	}

	static void entityRenderPreListener(WorldRenderContext ctx) {
		startIdentity(ctx);
		EntityRenderPreListener.invoke(ctx);
		endIdentity(ctx);
	}

	static void entityRenderPostListener(WorldRenderContext ctx) {
		startIdentity(ctx);
		EntityRenderPostListener.invoke(ctx);
		endIdentity(ctx);
	}

	static void translucentPostListener(WorldRenderContext ctx) {
		startIdentity(ctx);
		TranslucentPostListener.invoke(ctx);
		endIdentity(ctx);
	}

	public static void useViewStack(WorldRenderContext ctx, Runnable run) {
		final boolean apply = poseIsIdentity;

		if (apply) {
			endIdentity(ctx);
		}

		run.run();

		if (apply) {
			startIdentity(ctx);
		}
	}
}
