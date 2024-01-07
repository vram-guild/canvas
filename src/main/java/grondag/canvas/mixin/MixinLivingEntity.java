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

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import io.vram.frex.api.light.HeldItemLightListener;
import io.vram.frex.api.light.ItemLight;

import grondag.canvas.light.color.LightRegistry;
import grondag.canvas.light.color.entity.EntityLightProvider;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements EntityLightProvider {
	@Shadow public abstract void remove(Entity.RemovalReason removalReason);

	@Override
	public BlockPos canvas_getPos() {
		return ((LivingEntity) (Object) this).blockPosition();
	}

	@Override
	public short canvas_getLight() {
		final LivingEntity livingEntity = (LivingEntity) (Object) this;

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
