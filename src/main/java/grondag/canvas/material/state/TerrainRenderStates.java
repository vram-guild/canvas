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

package grondag.canvas.material.state;

import java.util.function.Predicate;

import net.minecraft.client.renderer.RenderType;

import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.rendertype.RenderTypeUtil;

import grondag.canvas.material.property.TargetRenderState;

/**
 * Here to defer initialization when referencing classes are loaded.
 */
public final class TerrainRenderStates {
	private TerrainRenderStates() { }

	public static CanvasRenderMaterial TRANSLUCENT_TERRAIN;
	public static RenderState TRANSLUCENT;
	public static RenderState SOLID;
	public static final Predicate<RenderState> TRANSLUCENT_PREDICATE = m -> m.target == TargetRenderState.TRANSLUCENT && m.primaryTargetTransparency;
	public static final Predicate<RenderState> SOLID_PREDICATE = m -> !TRANSLUCENT_PREDICATE.test(m);

	public static void onFirstShaderReload() {
		TRANSLUCENT_TERRAIN = (CanvasRenderMaterial) RenderTypeUtil.toMaterial(RenderType.translucent());
		TRANSLUCENT = TRANSLUCENT_TERRAIN.renderState();
		SOLID = ((CanvasRenderMaterial) RenderTypeUtil.toMaterial(RenderType.solid())).renderState();
	}

	static {
		// initialize with placeholder for things that tried to access it before shaders are loaded
		var standard = (CanvasRenderMaterial) MaterialFinder.threadLocal().find();
		TRANSLUCENT_TERRAIN = (CanvasRenderMaterial) MaterialFinder.threadLocal().find();
		TRANSLUCENT = SOLID = standard.renderState();
	}
}
