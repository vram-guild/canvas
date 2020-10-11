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

package grondag.canvas.mixin;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import grondag.canvas.mixinterface.ParticleExt;
import grondag.canvas.mixinterface.ParticleManagerExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.particle.ParticleEffect;

@Mixin(ParticleManager.class)
public class MixinParticleManager implements ParticleManagerExt {
	@Shadow private static List<ParticleTextureSheet> PARTICLE_TEXTURE_SHEETS;
	@Shadow private Map<ParticleTextureSheet, Queue<Particle>> particles;
	@Shadow private TextureManager textureManager;

	@Override
	public List<ParticleTextureSheet> canvas_textureSheets() {
		return PARTICLE_TEXTURE_SHEETS;
	}

	@Override
	public Map<ParticleTextureSheet, Queue<Particle>> canvas_particles() {
		return particles;
	}

	@Override
	public TextureManager canvas_textureManager() {
		return textureManager;
	}

	// let particles know their type
	@Inject(at = @At("RETURN"), method = "createParticle")
	private <T extends ParticleEffect> void afterCreateParticle(T parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> ci) {
		final Particle particle = ci.getReturnValue();

		if (particle != null) {
			((ParticleExt) particle).canvas_particleType(parameters.getType());
		}
	}
}
