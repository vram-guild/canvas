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
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import grondag.canvas.varia.CanvasMath;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Mixin(TextureSheetParticle.class)
public abstract class MixinSpriteBillboardParticle extends SingleQuadParticle {
	@Shadow protected TextureAtlasSprite sprite;

	protected MixinSpriteBillboardParticle(ClientLevel clientWorld, double d, double e, double f) {
		super(clientWorld, d, e, f);
	}

	private static final Quaternion quat = new Quaternion(0, 0, 0, 0);
	private static final Quaternion auxQuat = new Quaternion(0, 0, 0, 0);
	private static final Vector3f vec = new Vector3f();

	// slightly faster math and less allocation
	@Override
	public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
		final Vec3 vec3d = camera.getPosition();
		final float cx = (float) (Mth.lerp(tickDelta, xo, x) - vec3d.x());
		final float cy = (float) (Mth.lerp(tickDelta, yo, y) - vec3d.y());
		final float cz = (float) (Mth.lerp(tickDelta, zo, z) - vec3d.z());

		final Quaternion rotation;

		if (roll == 0.0F) {
			rotation = camera.rotation();
		} else {
			final Quaternion cr = camera.rotation();
			rotation = quat;
			rotation.set(cr.i(), cr.j(), cr.k(), cr.r());
			final float adjustedAngle = Mth.lerp(tickDelta, oRoll, roll);
			final Quaternion radialRotation = auxQuat;
			CanvasMath.setRadialRotation(radialRotation, Vector3f.ZP, adjustedAngle);
			rotation.mul(radialRotation);
		}

		final Vector3f pos = vec;
		final float scale = getQuadSize(tickDelta);
		final int light = getLightColor(tickDelta);

		final float l = getU0();
		final float m = getU1();
		final float n = getV0();
		final float o = getV1();

		vec.set(-1.0F, -1.0F, 0.0F);
		CanvasMath.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.x() * scale, cy + pos.y() * scale, cz + pos.z() * scale).uv(m, o).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();

		vec.set(-1.0F, 1.0F, 0.0F);
		CanvasMath.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.x() * scale, cy + pos.y() * scale, cz + pos.z() * scale).uv(m, n).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();

		vec.set(1.0F, 1.0F, 0.0F);
		CanvasMath.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.x() * scale, cy + pos.y() * scale, cz + pos.z() * scale).uv(l, n).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();

		vec.set(1.0F, -1.0F, 0.0F);
		CanvasMath.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.x() * scale, cy + pos.y() * scale, cz + pos.z() * scale).uv(l, o).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
		//		}
	}
}
