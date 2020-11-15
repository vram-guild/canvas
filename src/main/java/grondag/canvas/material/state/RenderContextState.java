/*
 * Copyright 2019, 2020 grondag
 *
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
 */

package grondag.canvas.material.state;

import java.util.function.Function;

import grondag.frex.api.material.BlockEntityMaterialMap;
import grondag.frex.api.material.EntityMaterialMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;

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
			blockState = blockEntity.getCachedState();
			blockEntityMap = BlockEntityMaterialMap.get(blockEntity.getType());
			activeFunc = blockEntityFunc;
		}
	}

	public RenderMaterialImpl mapMaterial(RenderMaterialImpl mat) {
		return activeFunc.apply(mat);
	}
}
