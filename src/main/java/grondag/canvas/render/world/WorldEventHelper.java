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
import com.mojang.math.Matrix3f;

import io.vram.frex.api.renderloop.EntityRenderPostListener;
import io.vram.frex.api.renderloop.EntityRenderPreListener;
import io.vram.frex.api.renderloop.TranslucentPostListener;
import io.vram.frex.api.renderloop.WorldRenderContext;

/**
 * Matrix insanity.
 *
 * <p>Canvas always applies view matrix on shader regardless of terrain or entity render.
 * Therefore, it needs to use identity pose stack and sets RenderSystem to use proper view matrix so that vanilla
 * renderers work as expected. However, third party renderers that use custom shader programs or draw calls
 * will fail without the appropriate compatibility.
 *
 * <p>Note that this class is NOT public API.
 */
public class WorldEventHelper {
	/**
	 * When true, pose stack pose matrix is identity, while RenderSystem has the correct view matrix.
	 */
	public static boolean poseIsIdentity;

	private static final Matrix3f normalMatrix = new Matrix3f();

	static void startIdentity(WorldRenderContext ctx) {
		normalMatrix.load(ctx.poseStack().last().normal());
		ctx.poseStack().setIdentity();
		poseIsIdentity = true;
	}

	static void endIdentity(WorldRenderContext ctx) {
		ctx.poseStack().last().pose().load(RenderSystem.getModelViewMatrix());
		ctx.poseStack().last().normal().load(normalMatrix);
		poseIsIdentity = false;
	}

	// Events not listed here hasn't been causing issues so far

	static void entityRenderPreListener(WorldRenderContext ctx) {
		// We don't correct matrix state now because renders that happen here typically use 3rd party render
		EntityRenderPreListener.invoke(ctx);
	}

	static void entityRenderPostListener(WorldRenderContext ctx) {
		EntityRenderPostListener.invoke(ctx);
	}

	static void translucentPostListener(WorldRenderContext ctx) {
		TranslucentPostListener.invoke(ctx);
	}

	/**
	 * Use this when you hate statefulness.
	 */
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

	/**
	 * Use this when you hate statefulness.
	 */
	public static void useIdentityStack(WorldRenderContext ctx, Runnable run) {
		final boolean apply = !poseIsIdentity;

		if (apply) {
			startIdentity(ctx);
		}

		run.run();

		if (apply) {
			endIdentity(ctx);
		}
	}
}
