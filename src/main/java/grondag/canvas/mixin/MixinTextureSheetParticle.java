/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import io.vram.frex.api.math.FrexMathUtil;

@Mixin(TextureSheetParticle.class)
public abstract class MixinTextureSheetParticle extends SingleQuadParticle {
	@Shadow protected TextureAtlasSprite sprite;

	protected MixinTextureSheetParticle(ClientLevel clientWorld, double d, double e, double f) {
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
			FrexMathUtil.setRadialRotation(radialRotation, Vector3f.ZP, adjustedAngle);
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
		FrexMathUtil.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.x() * scale, cy + pos.y() * scale, cz + pos.z() * scale).uv(m, o).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();

		vec.set(-1.0F, 1.0F, 0.0F);
		FrexMathUtil.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.x() * scale, cy + pos.y() * scale, cz + pos.z() * scale).uv(m, n).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();

		vec.set(1.0F, 1.0F, 0.0F);
		FrexMathUtil.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.x() * scale, cy + pos.y() * scale, cz + pos.z() * scale).uv(l, n).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();

		vec.set(1.0F, -1.0F, 0.0F);
		FrexMathUtil.applyBillboardRotation(pos, rotation);
		vertexConsumer.vertex(cx + pos.x() * scale, cy + pos.y() * scale, cz + pos.z() * scale).uv(l, o).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
		//		}
	}
}
