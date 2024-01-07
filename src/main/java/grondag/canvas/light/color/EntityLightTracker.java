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

import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityAccess;

import io.vram.frex.api.light.HeldItemLightListener;
import io.vram.frex.api.light.ItemLight;

import grondag.canvas.light.api.impl.BlockLightLoader;

public class EntityLightTracker {
	private static EntityLightTracker INSTANCE;
	private final Int2ObjectOpenHashMap<TrackedEntity<?>> entities = new Int2ObjectOpenHashMap<>();
	private final LightLevelAccess lightLevel;
	private boolean requiresInitialization = true;

	EntityLightTracker(LightLevelAccess lightLevel) {
		INSTANCE = this;
		this.lightLevel = lightLevel;
	}

	void update(ClientLevel level) {
		if (requiresInitialization) {
			requiresInitialization = false;

			var entities = level.entitiesForRendering();

			for (var entity:entities) {
				trackAny(entity);
			}
		}

		for (var entity : entities.values()) {
			entity.update();
		}
	}

	public static void levelAddsEntity(Entity entity) {
		if (INSTANCE != null && !INSTANCE.requiresInitialization) {
			INSTANCE.trackAny(entity);
		}
	}

	public static void levelRemovesEntity(int id) {
		if (INSTANCE != null && !INSTANCE.requiresInitialization) {
			INSTANCE.removeAny(id);
		}
	}

	void reload() {
		requiresInitialization = true;
		// might be called twice due to multiple hook (setLevel and allChanged). idempotent.
		removeAll();
	}

	void close(boolean lightLevelIsClosing) {
		if (!lightLevelIsClosing) {
			removeAll();
		}

		if (INSTANCE == this) {
			INSTANCE = null;
		}
	}

	private void removeAll() {
		// see notes on reload()
		for (var entity:entities.values()) {
			entity.removeLight();
		}

		entities.clear();
	}

	private void trackAny(Entity entity) {
		final TrackedEntity<?> trackedEntity;

		if (BlockLightLoader.ENTITY_LIGHTS.containsKey(entity.getType())) {
			final short loadedLight = BlockLightLoader.ENTITY_LIGHTS.get(entity.getType()).value;
			trackedEntity = new TrackedEntity<>(entity, e -> loadedLight);
		} else if (entity == Minecraft.getInstance().player) {
			// we already have shader held light
			return;
		} else if (entity instanceof LivingEntity livingEntity) {
			trackedEntity = new TrackedEntity<>(livingEntity, new HeldLightSupplier());
		} else if (entity instanceof ItemEntity itemEntity) {
			trackedEntity = new TrackedEntity<>(itemEntity, new ItemLightSupplier());
		} else {
			return;
		}

		entities.put(entity.getId(), trackedEntity);
	}

	private void removeAny(int id) {
		if (entities.containsKey(id)) {
			entities.remove(id).removeLight();
		}
	}

	// Unused at the moment
	private static class ThirdPersonSupplier implements Function<LocalPlayer, Short> {
		private final HeldLightSupplier heldLightSupplier = new HeldLightSupplier();

		@Override
		public Short apply(LocalPlayer localPlayer) {
			final boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
			return firstPerson ? 0 : heldLightSupplier.apply(localPlayer);
		}
	}

	private static class ItemLightSupplier implements Function<ItemEntity, Short> {
		@Override
		public Short apply(ItemEntity itemEntity) {
			ItemLight light = ItemLight.get(itemEntity.getItem());

			if (!light.worksInFluid() && itemEntity.isUnderWater()) {
				light = ItemLight.NONE;
			}

			return LightRegistry.encodeItem(light);
		}
	}

	private static class HeldLightSupplier implements Function<LivingEntity, Short> {
		@Override
		public Short apply(LivingEntity livingEntity) {
			final var mainItem = livingEntity.getMainHandItem();
			final var mainLight = ItemLight.get(mainItem);

			var light = HeldItemLightListener.apply(livingEntity, mainItem, mainLight);

			if (light.equals(ItemLight.NONE)) {
				final var offItem = livingEntity.getOffhandItem();
				final var offLight = ItemLight.get(offItem);
				light = HeldItemLightListener.apply(livingEntity, offItem, offLight);
			}

			if (!light.worksInFluid() && livingEntity.isUnderWater()) {
				light = ItemLight.NONE;
			}

			return LightRegistry.encodeItem(light);
		}
	}

	private class TrackedEntity<E extends EntityAccess> {
		private final E entity;
		private final Function<E, Short> lightSupplier;

		private final BlockPos.MutableBlockPos lastTrackedPos = new BlockPos.MutableBlockPos();
		private short lastTrackedLight = 0;

		TrackedEntity(E entity, Function<E, Short> lightSupplier) {
			this.entity = entity;
			this.lightSupplier = lightSupplier;
			lastTrackedPos.set(entity.blockPosition());
		}

		void update() {
			final BlockPos pos = entity.blockPosition();
			final short light = lightSupplier.apply(entity);
			final boolean changedLight = lastTrackedLight != light;
			final boolean changedPos = LightOp.lit(light) && !lastTrackedPos.equals(pos);

			if (changedLight || changedPos) {
				if (LightOp.lit(lastTrackedLight)) {
					lightLevel.removeVirtualLight(lastTrackedPos, lastTrackedLight);
				}

				if (LightOp.lit(light)) {
					lightLevel.placeVirtualLight(pos, light);
				}
			}

			lastTrackedLight = light;
			lastTrackedPos.set(pos);
		}

		public void removeLight() {
			if (LightOp.lit(lastTrackedLight)) {
				lightLevel.removeVirtualLight(entity.blockPosition(), lastTrackedLight);
			}
		}
	}
}
