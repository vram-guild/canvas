/*
 * Copyright © Contributing Authors
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

package grondag.canvas.material.state;

import java.util.function.Predicate;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;

import io.vram.frex.api.rendertype.RenderTypeUtil;

import grondag.canvas.material.property.TargetRenderState;

/**
 * Here to defer initialization when referencing classes are loaded.
 */
public final class TerrainRenderStates {
	private TerrainRenderStates() { }

	public static final RenderMaterialImpl TRANSLUCENT_TERRAIN = (RenderMaterialImpl) RenderTypeUtil.toMaterial(RenderType.translucent());
	public static final RenderState TRANSLUCENT = TRANSLUCENT_TERRAIN.renderState;
	public static final RenderState SOLID = ((RenderMaterialImpl) RenderTypeUtil.toMaterial(RenderType.solid())).renderState;
	public static final Predicate<RenderState> TRANSLUCENT_PREDICATE = m -> m.target == TargetRenderState.TRANSLUCENT && m.primaryTargetTransparency;
	public static final Predicate<RenderState> SOLID_PREDICATE = m -> !TRANSLUCENT_PREDICATE.test(m);

	static {
		assert TRANSLUCENT_TERRAIN.primaryTargetTransparency;

		// ensure item entity gets mapped to primary transparency
		assert ((RenderMaterialImpl) RenderTypeUtil.toMaterial(Sheets.translucentItemSheet())).primaryTargetTransparency;
	}
}
