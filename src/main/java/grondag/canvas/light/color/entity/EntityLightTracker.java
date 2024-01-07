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

package grondag.canvas.light.color.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import grondag.canvas.light.color.LightLevelAccess;
import grondag.canvas.light.color.LightOp;

public class EntityLightTracker {
	private static EntityLightTracker INSTANCE;
	private final Int2ObjectOpenHashMap<TrackedEntity> entities = new Int2ObjectOpenHashMap<>();
	private final LightLevelAccess lightLevel;
	private boolean requiresInitialization = true;

	public EntityLightTracker(LightLevelAccess lightLevel) {
		INSTANCE = this;
		this.lightLevel = lightLevel;
	}

	public void update(ClientLevel level) {
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

	public void reload() {
		requiresInitialization = true;
		// might be called twice due to multiple hook (setLevel and allChanged). idempotent.
		removeAll();
	}

	public void close(boolean lightLevelIsClosing) {
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
		try {
			EntityLightProvider lightProvider = (EntityLightProvider) entity;
			entities.put(entity.getId(), new TrackedEntity(lightProvider));
		} catch (ClassCastException ignored) {
			// eat 'em
		}
	}

	private void removeAny(int id) {
		if (entities.containsKey(id)) {
			entities.remove(id).removeLight();
		}
	}

	private class TrackedEntity {
		final EntityLightProvider entity;
		private final BlockPos.MutableBlockPos lastTrackedPos = new BlockPos.MutableBlockPos();
		private short lastTrackedLight = 0;

		TrackedEntity(EntityLightProvider entity) {
			this.entity = entity;
			lastTrackedPos.set(entity.canvas_getPos());
		}

		void update() {
			final BlockPos pos = entity.canvas_getPos();
			final short light = entity.canvas_getLight();
			final boolean changedLight = lastTrackedLight != light;
			final boolean changedPos = LightOp.emitter(light) && !lastTrackedPos.equals(pos);

			if (changedLight || changedPos) {
				if (LightOp.emitter(lastTrackedLight)) {
					lightLevel.removeVirtualLight(lastTrackedPos, lastTrackedLight);
				}

				if (LightOp.emitter(light)) {
					lightLevel.placeVirtualLight(pos, light);
				}

				System.out.println("Changed," + changedLight + "," + changedPos + "," + entity);
			}

			lastTrackedLight = light;
			lastTrackedPos.set(pos);
		}

		public void removeLight() {
			if (LightOp.emitter(lastTrackedLight)) {
				lightLevel.removeVirtualLight(entity.canvas_getPos(), lastTrackedLight);
			}
		}
	}
}
