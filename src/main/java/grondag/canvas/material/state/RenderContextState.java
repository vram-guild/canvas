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
