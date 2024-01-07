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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import grondag.canvas.light.color.entity.EntityLightTracker;

@Mixin(ClientLevel.class)
public class MixinClientLevel {
	@Inject(method = "addEntity", at = @At("TAIL"))
	void onAddEntity(int i, Entity entity, CallbackInfo ci) {
		EntityLightTracker.levelAddsEntity(entity);
	}

	@Inject(method = "removeEntity", at = @At("HEAD"))
	void onRemoveEntity(int id, Entity.RemovalReason removalReason, CallbackInfo ci) {
		EntityLightTracker.levelRemovesEntity(id);
	}
}
