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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The vanilla fish model has an excessively large tail despite the texture suggesting otherwise.
 * This causes the texture UV to exceed 1, and since we don't support that we just fix the model directly.
 *
 * <p>The alternative is to extend the vertex UV format, but we couldn't do that for only a singular aquatic friend, could we?
 */
@Mixin(TropicalFishModelA.class)
public class MixinTropicalFishModelA {
	private static CubeDeformation _cubeDeformation;

	@Inject(method = "createBodyLayer(Lnet/minecraft/client/model/geom/builders/CubeDeformation;)Lnet/minecraft/client/model/geom/builders/LayerDefinition;", at = @At(value = "HEAD"))
	private static void beforePart(CubeDeformation cubeDeformation, CallbackInfoReturnable<LayerDefinition> cir) {
		_cubeDeformation = cubeDeformation;
	}

	@Redirect(method = "createBodyLayer(Lnet/minecraft/client/model/geom/builders/CubeDeformation;)Lnet/minecraft/client/model/geom/builders/LayerDefinition;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/builders/PartDefinition;addOrReplaceChild(Ljava/lang/String;Lnet/minecraft/client/model/geom/builders/CubeListBuilder;Lnet/minecraft/client/model/geom/PartPose;)Lnet/minecraft/client/model/geom/builders/PartDefinition;"))
	private static PartDefinition onPart(PartDefinition instance, String string, CubeListBuilder cubeListBuilder, PartPose partPose) {
		if (string.equals("tail")) {
			final var result = instance.addOrReplaceChild(string, CubeListBuilder.create().texOffs(24, -4).addBox(0.0F, -1.5F, 0.0F, 0.0F, 3.0F, 4.0F, _cubeDeformation), partPose);
			_cubeDeformation = null;
			return result;
		} else {
			return instance.addOrReplaceChild(string, cubeListBuilder, partPose);
		}
	}
}
