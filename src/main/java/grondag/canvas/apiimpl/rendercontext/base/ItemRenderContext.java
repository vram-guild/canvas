/*
 * Copyright © Contributing Authors
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

package grondag.canvas.apiimpl.rendercontext.base;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.model.ItemModel;
import io.vram.frex.base.renderer.context.BaseItemContext;

public abstract class ItemRenderContext extends AbstractBakedRenderContext<BaseItemContext> {
	@Override
	protected BaseItemContext createInputContext() {
		return new BaseItemContext();
	}

	protected abstract void prepareEncoding(MultiBufferSource vertexConsumers);

	@SuppressWarnings("resource")
	public void renderItem(ItemModelShaper models, ItemStack stack, TransformType renderMode, boolean isLeftHand, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, int overlay, BakedModel model) {
		if (stack.isEmpty()) return;
		final boolean detachedPerspective = renderMode == ItemTransforms.TransformType.GUI || renderMode == ItemTransforms.TransformType.GROUND || renderMode == ItemTransforms.TransformType.FIXED;

		if (detachedPerspective) {
			if (stack.is(Items.TRIDENT)) {
				model = models.getItemModel(Items.TRIDENT);
			} else if (stack.is(Items.SPYGLASS)) {
				model = models.getItemModel(Items.SPYGLASS);
			}
		}

		final MatrixStack matrixStack = MatrixStack.cast(poseStack);
		inputContext.prepareForItem(model, stack, renderMode, light, overlay, isLeftHand, matrixStack);
		materialMap = MaterialMap.get(stack);
		matrixStack.push();

		// PERF: optimize matrix stack operations
		model.getTransforms().getTransform(renderMode).apply(isLeftHand, poseStack);
		matrixStack.translate(-0.5f, -0.5f, -0.5f);

		prepareEncoding(vertexConsumers);

		if (model.isCustomRenderer() || stack.getItem() == Items.TRIDENT && !detachedPerspective) {
			renderCustomModel(Minecraft.getInstance().getItemRenderer().blockEntityRenderer, vertexConsumers);
		} else {
			((ItemModel) model).renderAsItem(inputContext, emitter());
		}

		matrixStack.pop();
	}

	protected abstract void renderCustomModel(BlockEntityWithoutLevelRenderer builtInRenderer, MultiBufferSource vertexConsumers);

	@Override
	protected void adjustMaterial() {
		final MaterialFinder finder = this.finder;

		finder.foilOverlay(inputContext.itemStack().hasFoil());

		if (inputContext.overlay() != OverlayTexture.NO_OVERLAY) {
			finder.overlay(inputContext.overlay());
		}

		int preset = finder.preset();

		// fully specific renderable material
		if (preset == MaterialConstants.PRESET_NONE) return;

		if (preset == MaterialConstants.PRESET_DEFAULT) {
			preset = inputContext.defaultPreset();
			finder.preset(MaterialConstants.PRESET_NONE);
		}

		switch (preset) {
			case MaterialConstants.PRESET_CUTOUT:
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_HALF)
					.unmipped(true)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			case MaterialConstants.PRESET_CUTOUT_MIPPED:
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_HALF)
					.unmipped(false)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			case MaterialConstants.PRESET_TRANSLUCENT:
				// Note on glint rendering
				// Glint renders use EQUALS depth test.
				// This makes it important that
				//   1) geometry is the same
				//   2) cutout is enabled for generated models so depth buffer isn't updated
				// 1 is easily solved by rendering twice with same vertex data
				// 2 has to be finessed because blend mode = TRANSLUCENT doesn't make it clear cutout is needed.
				// The code below is an ugly hack - need a better way

				finder.transparency(MaterialConstants.TRANSPARENCY_TRANSLUCENT)
					.cutout(inputContext.isBlockItem() ? MaterialConstants.CUTOUT_NONE : MaterialConstants.CUTOUT_TENTH)
					.unmipped(false)
					.target(inputContext.drawTranslucencyToMainTarget() ? MaterialConstants.TARGET_MAIN : MaterialConstants.TARGET_ENTITIES)
					.sorted(!inputContext.drawTranslucencyToMainTarget());
				break;
			case MaterialConstants.PRESET_SOLID:
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_NONE)
					.unmipped(false)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			default:
				assert false : "Unhandled blend mode";
		}

		if (inputContext.isFrontLit()) {
			finder.disableDiffuse(true);
		}

		finder.disableAo(true);
	}
}
