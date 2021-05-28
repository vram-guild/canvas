/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import grondag.canvas.varia.CanvasMath;

@Mixin(SpriteBillboardParticle.class)
public abstract class MixinSpriteBillboardParticle extends BillboardParticle {
	@Shadow protected Sprite sprite;

	protected MixinSpriteBillboardParticle(ClientWorld clientWorld, double d, double e, double f) {
		super(clientWorld, d, e, f);
	}

	private static final Quaternion quat = new Quaternion(0, 0, 0, 0);
	private static final Quaternion auxQuat = new Quaternion(0, 0, 0, 0);
	private static final Vec3f vec = new Vec3f();

	// slightly faster math and less allocation
	@Override
	public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
		final Vec3d vec3d = camera.getPos();
		final float cx = (float) (MathHelper.lerp(tickDelta, prevPosX, x) - vec3d.getX());
		final float cy = (float) (MathHelper.lerp(tickDelta, prevPosY, y) - vec3d.getY());
		final float cz = (float) (MathHelper.lerp(tickDelta, prevPosZ, z) - vec3d.getZ());

		final Quaternion rotation;

		if (angle == 0.0F) {
			rotation = camera.getRotation();
		} else {
			final Quaternion cr = camera.getRotation();
			rotation = quat;
			rotation.set(cr.getX(), cr.getY(), cr.getZ(), cr.getW());
			final float adjustedAngle = MathHelper.lerp(tickDelta, prevAngle, angle);
			final Quaternion radialRotation = auxQuat;
			CanvasMath.setRadialRotation(radialRotation, Vec3f.POSITIVE_Z, adjustedAngle);
			rotation.hamiltonProduct(radialRotation);
		}

		final Vec3f pos = vec;
		final float scale = getSize(tickDelta);
		final int light = getBrightness(tickDelta);

		final float l = getMinU();
		final float m = getMaxU();
		final float n = getMinV();
		final float o = getMaxV();

		vec.set(-1.0F, -1.0F, 0.0F);
		CanvasMath.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.getX() * scale, cy + pos.getY() * scale, cz + pos.getZ() * scale).texture(m, o).color(colorRed, colorGreen, colorBlue, colorAlpha).light(light).next();

		vec.set(-1.0F, 1.0F, 0.0F);
		CanvasMath.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.getX() * scale, cy + pos.getY() * scale, cz + pos.getZ() * scale).texture(m, n).color(colorRed, colorGreen, colorBlue, colorAlpha).light(light).next();

		vec.set(1.0F, 1.0F, 0.0F);
		CanvasMath.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.getX() * scale, cy + pos.getY() * scale, cz + pos.getZ() * scale).texture(l, n).color(colorRed, colorGreen, colorBlue, colorAlpha).light(light).next();

		vec.set(1.0F, -1.0F, 0.0F);
		CanvasMath.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.getX() * scale, cy + pos.getY() * scale, cz + pos.getZ() * scale).texture(l, o).color(colorRed, colorGreen, colorBlue, colorAlpha).light(light).next();
		//		}
	}
}
