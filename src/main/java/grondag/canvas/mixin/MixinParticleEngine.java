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

import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleOptions;

import grondag.canvas.mixinterface.ParticleEngineExt;
import grondag.canvas.mixinterface.ParticleExt;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine implements ParticleEngineExt {
	@Shadow private static List<ParticleRenderType> RENDER_ORDER;
	@Shadow private Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow private TextureManager textureManager;

	@Override
	public List<ParticleRenderType> canvas_textureSheets() {
		return RENDER_ORDER;
	}

	@Override
	public Map<ParticleRenderType, Queue<Particle>> canvas_particles() {
		return particles;
	}

	@Override
	public TextureManager canvas_textureManager() {
		return textureManager;
	}

	// let particles know their type
	@Inject(at = @At("RETURN"), method = "makeParticle")
	private <T extends ParticleOptions> void afterMakeParticle(T parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> ci) {
		final Particle particle = ci.getReturnValue();

		if (particle != null) {
			((ParticleExt) particle).canvas_particleType(parameters.getType());
		}
	}
}
