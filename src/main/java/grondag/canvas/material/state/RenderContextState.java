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

import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.material.BlockEntityMaterialMap;
import io.vram.frex.api.material.EntityMaterialMap;

public class RenderContextState {
	private EntityMaterialMap entityMap = null;
	private BlockEntityMaterialMap blockEntityMap = null;
	private Entity entity;
	private BlockState blockState;
	private final MaterialFinderImpl finder = new MaterialFinderImpl();

	private final Function<RenderMaterialImpl, RenderMaterialImpl> defaultFunc = m -> m;
	private final Function<RenderMaterialImpl, RenderMaterialImpl> entityFunc = m -> (RenderMaterialImpl) entityMap.getMapped(m, entity, finder);
	private final Function<RenderMaterialImpl, RenderMaterialImpl> blockEntityFunc = m -> (RenderMaterialImpl) blockEntityMap.getMapped(m, blockState, finder);

	private Function<RenderMaterialImpl, RenderMaterialImpl> activeFunc = defaultFunc;
	private BiFunction<MaterialFinderImpl, RenderMaterialImpl, RenderMaterialImpl> guiFunc = GuiMode.NORMAL.func;

	public void setCurrentEntity(@Nullable Entity entity) {
		if (entity == null) {
			entityMap = null;
			activeFunc = defaultFunc;
		} else {
			entityMap = EntityMaterialMap.get(entity.getType());
			activeFunc = entityFunc;
		}
	}

	public void setCurrentBlockEntity(@Nullable BlockEntity blockEntity) {
		if (blockEntity == null) {
			blockEntityMap = null;
			activeFunc = defaultFunc;
		} else {
			blockState = blockEntity.getBlockState();
			blockEntityMap = BlockEntityMaterialMap.get(blockEntity.getType());
			activeFunc = blockEntityFunc;
		}
	}

	public void guiMode(GuiMode guiMode) {
		guiFunc = guiMode.func;
	}

	public RenderMaterialImpl mapMaterial(RenderMaterialImpl mat) {
		return guiFunc.apply(finder, activeFunc.apply(mat));
	}

	public enum GuiMode {
		NORMAL((f, m) -> m),
		GUI_FRONT_LIT((f, m) -> f.copyFrom(m).disableDiffuse(true).find());

		private final BiFunction<MaterialFinderImpl, RenderMaterialImpl, RenderMaterialImpl> func;

		GuiMode(BiFunction<MaterialFinderImpl, RenderMaterialImpl, RenderMaterialImpl> func) {
			this.func = func;
		}
	}
}
