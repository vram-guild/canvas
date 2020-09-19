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

import grondag.canvas.shader.wip.encoding.WipVertexCollectorImpl;
import grondag.canvas.varia.CanvasMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static grondag.canvas.shader.wip.encoding.WipVertexCollector.NORMALIZED_U0_V0;
import static grondag.canvas.shader.wip.encoding.WipVertexCollector.NORMALIZED_U0_V1;
import static grondag.canvas.shader.wip.encoding.WipVertexCollector.NORMALIZED_U1_V0;
import static grondag.canvas.shader.wip.encoding.WipVertexCollector.NORMALIZED_U1_V1;
import static grondag.canvas.shader.wip.encoding.WipVertexCollector.packColorFromFloats;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

@Mixin(SpriteBillboardParticle.class)
public abstract class MixinSpriteBillboardParticle extends BillboardParticle {
	@Shadow protected Sprite sprite;

	protected MixinSpriteBillboardParticle(ClientWorld clientWorld, double d, double e, double f) {
		super(clientWorld, d, e, f);
	}

	private static final Quaternion quat = new Quaternion(0, 0, 0, 0);
	private static final Quaternion auxQuat = new Quaternion(0, 0, 0, 0);
	private static final Vector3f vec = new Vector3f();

	@Override
	public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
		if (CanvasMath.hack()) {
			final Vec3d vec3d = camera.getPos();
			final float cx = (float) (MathHelper.lerp(tickDelta, prevPosX, x) - vec3d.getX());
			final float cy = (float) (MathHelper.lerp(tickDelta, prevPosY, y) - vec3d.getY());
			final float cz = (float) (MathHelper.lerp(tickDelta, prevPosZ, z) - vec3d.getZ());

			final Quaternion cr = camera.getRotation();
			final Quaternion rotation = quat;
			rotation.set(cr.getX(), cr.getY(), cr.getZ(), cr.getW());

			if (angle != 0.0F) {
				final float adjustedAngle = MathHelper.lerp(tickDelta, prevAngle, angle);
				final Quaternion radialRotation = auxQuat;
				CanvasMath.setRadialRotation(radialRotation, Vector3f.POSITIVE_Z, adjustedAngle);
				rotation.hamiltonProduct(radialRotation);
			}

			final Vector3f pos = vec;
			final float scale = getSize(tickDelta);
			final int light = getColorMultiplier(tickDelta);

			if (vertexConsumer instanceof WipVertexCollectorImpl) {
				// WIP: hook ParticleManager.renderParticles so we get a collector here - better yet call a different method
				final WipVertexCollectorImpl vc = (WipVertexCollectorImpl) vertexConsumer;
				vc.sprite(sprite);

				final int color = packColorFromFloats(colorRed, colorGreen, colorBlue, colorAlpha);

				vec.set(-1.0F, -1.0F, 0.0F);
				CanvasMath.applyRotation(pos, rotation);
				pos.scale(scale);
				pos.add(cx, cy, cz);
				vc.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(NORMALIZED_U1_V1).color(color).light(light).next();

				vec.set(-1.0F, 1.0F, 0.0F);
				CanvasMath.applyRotation(pos, rotation);
				pos.scale(scale);
				pos.add(cx, cy, cz);
				vc.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(NORMALIZED_U1_V0).color(color).light(light).next();

				vec.set(1.0F, 1.0F, 0.0F);
				CanvasMath.applyRotation(pos, rotation);
				pos.scale(scale);
				pos.add(cx, cy, cz);
				vc.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(NORMALIZED_U0_V0).color(color).light(light).next();

				vec.set(1.0F, -1.0F, 0.0F);
				CanvasMath.applyRotation(pos, rotation);
				pos.scale(scale);
				pos.add(cx, cy, cz);
				vc.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(NORMALIZED_U0_V1).color(color).light(light).next();
			} else {
				final float l = getMinU();
				final float m = getMaxU();
				final float n = getMinV();
				final float o = getMaxV();

				vec.set(-1.0F, -1.0F, 0.0F);
				CanvasMath.applyRotation(pos, rotation);
				pos.scale(scale);
				pos.add(cx, cy, cz);
				vertexConsumer.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(m, o).color(colorRed, colorGreen, colorBlue, colorAlpha).light(light).next();

				vec.set(-1.0F, 1.0F, 0.0F);
				CanvasMath.applyRotation(pos, rotation);
				pos.scale(scale);
				pos.add(cx, cy, cz);
				vertexConsumer.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(m, n).color(colorRed, colorGreen, colorBlue, colorAlpha).light(light).next();

				vec.set(1.0F, 1.0F, 0.0F);
				CanvasMath.applyRotation(pos, rotation);
				pos.scale(scale);
				pos.add(cx, cy, cz);
				vertexConsumer.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(l, n).color(colorRed, colorGreen, colorBlue, colorAlpha).light(light).next();

				vec.set(1.0F, -1.0F, 0.0F);
				CanvasMath.applyRotation(pos, rotation);
				pos.scale(scale);
				pos.add(cx, cy, cz);
				vertexConsumer.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(l, o).color(colorRed, colorGreen, colorBlue, colorAlpha).light(light).next();
			}
		} else {
			super.buildGeometry(vertexConsumer, camera, tickDelta);
		}
	}
}
