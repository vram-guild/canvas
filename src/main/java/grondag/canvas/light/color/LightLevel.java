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

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;

class LightLevel implements LightLevelAccess {
	private BlockAndTintGetter baseLevel = null;
	private EntityLightTracker entityLightTracker = null;

	private final Long2ObjectOpenHashMap<ShortArrayList> virtualLights = new Long2ObjectOpenHashMap<>();
	private final LongArrayFIFOQueue virtualQueue = new LongArrayFIFOQueue();
	private final ObjectOpenHashSet<LightRegion> virtualCheckQueue = new ObjectOpenHashSet<>();

	private static final Operator DO_NOTHING = (pos, light) -> { };
	private static final Getter GET_ZERO = pos -> (short) 0;

	private Operator placer = DO_NOTHING;
	private Operator remover = DO_NOTHING;
	private Getter getter = GET_ZERO;

	LightLevel() {
		reload();

		// dummy test
		// placeVirtualLight(new BlockPos(-278, 43, 6), LightOp.encodeLight(0x0, 0xf, 0x0, false, true, false));
	}

	public void reload() {
		assert Pipeline.config().coloredLights != null;

		final boolean virtualAllowed = Pipeline.config().coloredLights.allowVirtual;

		if (Configurator.entityLightSource && virtualAllowed) {
			if (entityLightTracker == null) {
				entityLightTracker = new EntityLightTracker(this);
			} else {
				// no need to clear as we are nuking everything
				entityLightTracker.reload(false);
			}
		} else {
			if (entityLightTracker != null) {
				// no need to clear as we are nuking everything
				entityLightTracker.close(false);
			}

			entityLightTracker = null;
		}

		// NB: implementations are expected to repopulate every time chunk is reloaded
		virtualLights.clear();

		if (virtualAllowed) {
			placer = PLACE_VIRTUAL;
			remover = REMOVE_VIRTUAL;
			getter = GET_VIRTUAL;
		} else {
			placer = DO_NOTHING;
			remover = DO_NOTHING;
			getter = GET_ZERO;
		}
	}

	void updateOnStartFrame(ClientLevel level) {
		if (baseLevel != level) {
			baseLevel = level;

			if (entityLightTracker != null) {
				entityLightTracker.reload(true);
			}
		}

		if (entityLightTracker != null) {
			entityLightTracker.update(level);
		}

		updateInner();
	}

	@Override
	public BlockAndTintGetter level() {
		return baseLevel;
	}

	@Override
	public short getRegistered(BlockPos pos) {
		var registered = baseLevel == null ? 0 : LightRegistry.get(baseLevel.getBlockState(pos));
		return combineWithBlockLight(registered, getter.apply(pos));
	}

	@Override
	public short getRegistered(BlockPos pos, @Nullable BlockState state) {
		// MAINTENANCE NOTICE: this function is a special casing of getRegistered(BlockPos)
		var registered = state != null ? LightRegistry.get(state) : (baseLevel == null ? 0 : LightRegistry.get(baseLevel.getBlockState(pos)));
		return combineWithBlockLight(registered, getter.apply(pos));
	}

	@Override
	public void placeVirtualLight(BlockPos blockPos, short light) {
		placer.apply(blockPos, light);
	}

	@Override
	public void removeVirtualLight(BlockPos blockPos, short light) {
		remover.apply(blockPos, light);
	}

	private final Operator PLACE_VIRTUAL = (blockPos, light) -> {
		final long pos = blockPos.asLong();
		final var list = virtualLights.computeIfAbsent(pos, l -> new ShortArrayList());
		list.add(encodeVirtualLight(light));
		virtualQueue.enqueue(pos);
	};

	private final Operator REMOVE_VIRTUAL = (blockPos, light) -> {
		final long pos = blockPos.asLong();
		final var list = virtualLights.get(pos);
		final int i = list == null ? -1 : list.indexOf(encodeVirtualLight(light));

		if (i != -1) {
			list.removeShort(i);
			virtualQueue.enqueue(pos);
		}
	};

	void close() {
		if (entityLightTracker != null) {
			// no need to clear as we are nuking everything
			entityLightTracker.close(false);
			entityLightTracker = null;
		}
	}

	private final Getter GET_VIRTUAL = blockPos -> {
		ShortArrayList lights = virtualLights.get(blockPos.asLong());

		if (lights != null) {
			short combined = 0;

			for (short light : lights) {
				combined = LightOp.max(light, combined);
			}

			return combined;
		}

		return (short) 0;
	};

	private void updateInner() {
		while (!virtualQueue.isEmpty()) {
			long pos = virtualQueue.dequeueLong();
			var blockPos = BlockPos.of(pos);
			var region = LightDataManager.INSTANCE.getFromBlock(blockPos);

			if (region != null) {
				region.checkBlock(blockPos, null);
				virtualCheckQueue.add(region);
			} else if (virtualLights.containsKey(pos)) {
				// there are virtual lights (placed and never removed) but the region doesn't exist
				CanvasMod.LOG.warn("Trying to update virtual lights on a null region. ID:" + pos);
			}
		}

		for (var region : virtualCheckQueue) {
			region.markUrgent();
			region.submitChecks();
		}

		virtualCheckQueue.clear();
	}

	public static short encodeVirtualLight(short light) {
		return LightOp.encodeLight(LightOp.pure(light), false, true, false);
	}

	private static short combineWithBlockLight(short block, short virtual) {
		return LightOp.makeEmitter(LightOp.max(block, virtual));
	}

	@FunctionalInterface
	private interface Operator {
		void apply(BlockPos pos, short light);
	}

	@FunctionalInterface
	private interface Getter {
		short apply(BlockPos pos);
	}
}
