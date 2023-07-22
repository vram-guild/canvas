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

package grondag.canvas.light.color;

import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import io.vram.frex.api.light.ItemLight;

import grondag.canvas.light.api.impl.BlockLightLoader;

public class LightRegistry {
	private static final ConcurrentHashMap<BlockState, Short> cachedLights = new ConcurrentHashMap<>();
	// Only for full block checks. Real MC world is only needed for blocks with positional offset like flowers, etc.
	private static final DummyWorld DUMMY_WORLD = new DummyWorld();

	public static void reload(ResourceManager manager) {
		cachedLights.clear();
		BlockLightLoader.reload(manager);
	}

	public static short get(BlockState blockState) {
		// maybe just populate it during reload? don't want to slow down resource reload though
		return cachedLights.computeIfAbsent(blockState, LightRegistry::generate);
	}

	private static short generate(BlockState blockState) {
		final boolean isFullCube = blockState.isCollisionShapeFullBlock(DUMMY_WORLD.set(blockState), DummyWorld.origin);
		final int lightLevel = blockState.getLightEmission();
		final short defaultLight = LightOp.encodeLight(lightLevel, lightLevel, lightLevel, isFullCube, lightLevel > 0, blockState.canOcclude());

		BlockLightLoader.CachedBlockLight apiLight = BlockLightLoader.INSTANCE.blockLights.get(blockState);

		if (apiLight == null && !blockState.getFluidState().isEmpty()) {
			apiLight = BlockLightLoader.INSTANCE.fluidLights.get(blockState.getFluidState());
		}

		if (apiLight != null) {
			if (!apiLight.levelIsSet()) {
				apiLight = apiLight.withLevel(blockState.getLightEmission());
			}

			return LightOp.encodeLight(apiLight.value(), isFullCube, apiLight.value() != 0, blockState.canOcclude());
		}

		if (lightLevel < 1) {
			return defaultLight;
		}

		// Item Light color-only fallback (feature?)
		final ItemStack stack = new ItemStack(blockState.getBlock(), 1);
		final ItemLight itemLight = ItemLight.get(stack);

		if (itemLight == null) {
			return defaultLight;
		}

		float maxValue = Math.max(itemLight.red(), Math.max(itemLight.green(), itemLight.blue()));

		if (maxValue <= 0) {
			return defaultLight;
		}

		final int r = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.red() / maxValue));
		final int g = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.green() / maxValue));
		final int b = org.joml.Math.clamp(1, 15, Math.round(lightLevel * itemLight.blue() / maxValue));

		return LightOp.encodeLight(r, g, b, isFullCube, true, blockState.canOcclude());
	}

	private static class DummyWorld implements BlockGetter {
		private static final BlockPos origin = new BlockPos(0, 0, 0);
		private static BlockState state;

		private DummyWorld set(BlockState state) {
			DummyWorld.state = state;
			return this;
		}

		@Nullable
		@Override
		public BlockEntity getBlockEntity(BlockPos blockPos) {
			return null;
		}

		@Override
		public BlockState getBlockState(BlockPos blockPos) {
			return blockPos.equals(origin) ? state : Blocks.AIR.defaultBlockState();
		}

		@Override
		public FluidState getFluidState(BlockPos blockPos) {
			return blockPos.equals(origin) ? state.getFluidState() : Fluids.EMPTY.defaultFluidState();
		}

		@Override
		public int getHeight() {
			return 1;
		}

		@Override
		public int getMinBuildHeight() {
			return 0;
		}
	}
}
