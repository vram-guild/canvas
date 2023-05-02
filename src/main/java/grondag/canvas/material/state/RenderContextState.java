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

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;

public class RenderContextState {
	@SuppressWarnings("rawtypes")
	private MaterialMap activeMap = MaterialMap.IDENTITY;
	@SuppressWarnings("rawtypes")
	private MaterialMap swapMap = null;
	private Object activeGameObject = null;
	private final MaterialFinder finder = MaterialFinder.newInstance();

	private MaterialMap<Entity> entityMap = MaterialMap.identity();
	private MaterialMap<BlockState> blockEntityMap = MaterialMap.identity();
	private MaterialMap<ItemStack> itemMap = MaterialMap.identity();
	private GuiMode guiMode = GuiMode.NORMAL;
	private boolean renderingItem = false;
	private boolean glintEntity = false;

	public void setCurrentEntity(@Nullable Entity entity) {
		entityMap = entity == null ? MaterialMap.identity() : MaterialMap.get(entity.getType());
		activeGameObject = entity;
		activeMap = entityMap;
		glintEntity = entity != null;
	}

	public void setCurrentBlockEntity(@Nullable BlockEntity blockEntity) {
		if (blockEntity == null) {
			activeGameObject = Blocks.AIR.defaultBlockState();
			blockEntityMap = MaterialMap.identity();
		} else {
			activeGameObject = blockEntity.getBlockState();
			blockEntityMap = MaterialMap.get(blockEntity.getType());
		}

		activeMap = blockEntityMap;
	}

	/**
	 * For items with custom renderer.
	 *
	 * @param itemStack the item stack
	 */
	public void pushItemState(ItemStack itemStack) {
		assert swapMap == null;
		itemMap = MaterialMap.get(itemStack);
		swapMap = activeMap; // preserve active function
		activeMap = itemMap;
		renderingItem = true;
		glintEntity = itemStack.is(Items.TRIDENT);
	}

	public void popItemState() {
		itemMap = MaterialMap.identity();
		activeMap = swapMap;
		swapMap = null;
		renderingItem = false;
	}

	public void guiMode(GuiMode guiMode) {
		this.guiMode = guiMode;
	}

	@SuppressWarnings("unchecked")
	public CanvasRenderMaterial mapMaterial(CanvasRenderMaterial mat) {
		if (activeMap.isIdentity() && guiMode == GuiMode.NORMAL) {
			if (mat.foilOverlay() && glintEntity) {
				return (CanvasRenderMaterial) finder.copyFrom(mat).glintEntity(true).find();
			}

			return mat;
		} else {
			finder.copyFrom(mat);

			if (renderingItem) {
				activeMap.map(finder, null);
				finder.textureIndex(mat.textureIndex()); // custom item wants to retain custom texture
			} else {
				activeMap.map(finder, activeGameObject);
			}

			finder.glintEntity(glintEntity);
			guiMode.apply(finder);
			return (CanvasRenderMaterial) finder.find();
		}
	}

	public enum GuiMode {
		NORMAL() {
			@Override void apply(MaterialFinder finder) { }
		},
		GUI_NORMAL() {
			@Override void apply(MaterialFinder finder) {
				finder.fog(false);
			}
		},
		GUI_FRONT_LIT() {
			@Override void apply(MaterialFinder finder) {
				finder.fog(false).disableDiffuse(true);
			}
		};

		abstract void apply(MaterialFinder finder);
	}
}
