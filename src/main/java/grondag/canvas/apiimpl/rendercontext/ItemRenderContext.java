/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
 ******************************************************************************/


package grondag.canvas.apiimpl.rendercontext;

import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelTransformation.Mode;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.encoding.VertexEncoder;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.mixinterface.Matrix3fExt;

/**
 * The render context used for item rendering.
 * Does not implement emissive lighting for sake
 * of simplicity in the default renderer.
 */
public class ItemRenderContext extends AbstractRenderContext implements RenderContext {
	/** Value vanilla uses for item rendering.  The only sensible choice, of course.  */
	private static final long ITEM_RANDOM_SEED = 42L;

	private final ItemColors colorMap;
	private final Random random = new Random();

	private VertexConsumerProvider vertexConsumerProvider;
	private VertexConsumer modelVertexConsumer;

	private BlendMode quadBlendMode;
	private VertexConsumer quadVertexConsumer;

	private Mode transformMode;
	private int lightmap;
	private ItemStack itemStack;

	private final Supplier<Random> randomSupplier = () -> {
		final Random result = random;
		result.setSeed(ITEM_RANDOM_SEED);
		return random;
	};

	public ItemRenderContext(ItemColors colorMap) {
		this.colorMap = colorMap;
	}

	public void renderModel(ItemStack itemStack, Mode transformMode, boolean invert, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int lightmap, int overlay, FabricBakedModel model) {
		this.lightmap = lightmap;
		this.overlay = overlay;
		this.itemStack = itemStack;
		this.vertexConsumerProvider = vertexConsumerProvider;
		this.transformMode = transformMode;

		quadBlendMode = BlendMode.DEFAULT;
		modelVertexConsumer = selectVertexConsumer(RenderLayers.getItemLayer(itemStack));
		matrixStack.push();

		((BakedModel) model).getTransformation().getTransformation(transformMode).apply(invert, matrixStack);
		matrixStack.translate(-0.5D, -0.5D, -0.5D);
		matrix = matrixStack.peek().getModel();
		normalMatrix = (Matrix3fExt)(Object) matrixStack.peek().getNormal();

		model.emitItemQuads(itemStack, randomSupplier, this);

		matrixStack.pop();

		this.itemStack = null;
		modelVertexConsumer = null;
	}

	/**
	 * Use non-culling translucent material in GUI to match vanilla behavior. If the item
	 * is enchanted then also select a dual-output vertex consumer. For models with layered
	 * coplanar polygons this means we will render the glint more than once. Indigo doesn't
	 * support sprite layers, so this can't be helped in this implementation.
	 */
	private VertexConsumer selectVertexConsumer(RenderLayer layerIn) {
		final RenderLayer layer = transformMode == ModelTransformation.Mode.GUI && Objects.equals(layerIn, TexturedRenderLayers.getEntityTranslucent()) ? TexturedRenderLayers.getEntityTranslucentCull() : layerIn;
		return ItemRenderer.getArmorVertexConsumer(vertexConsumerProvider, layer, true, itemStack.hasEnchantmentGlint());
	}

	/**
	 * Caches custom blend mode / vertex consumers and mimics the logic
	 * in {@code RenderLayers.getEntityBlockLayer}. Layers other than
	 * translucent are mapped to cutout.
	 */
	private VertexConsumer quadVertexConsumer(BlendMode blendMode) {
		if (blendMode == BlendMode.DEFAULT) {
			return modelVertexConsumer;
		}

		if (blendMode != BlendMode.TRANSLUCENT) {
			blendMode = BlendMode.CUTOUT;
		}

		if (blendMode == quadBlendMode) {
			return quadVertexConsumer;
		} else if (blendMode == BlendMode.TRANSLUCENT) {
			quadVertexConsumer = selectVertexConsumer(TexturedRenderLayers.getEntityTranslucent());
			quadBlendMode = BlendMode.TRANSLUCENT;
		} else {
			quadVertexConsumer = selectVertexConsumer(TexturedRenderLayers.getEntityCutout());
			quadBlendMode = BlendMode.CUTOUT;
		}

		return quadVertexConsumer;
	}

	@Override
	protected boolean cullTest(Direction face) {
		return true;
	}

	@Override
	public MaterialContext materialContext() {
		return MaterialContext.ITEM;
	}

	@Override
	protected Random random() {
		return randomSupplier.get();
	}

	@Override
	protected boolean defaultAo() {
		return false;
	}

	@Override
	protected BlockState blockState() {
		return null;
	}

	@Override
	public int overlay() {
		return overlay;
	}

	@Override
	public Matrix4f matrix() {
		return matrix;
	}

	@Override
	public Matrix3fExt normalMatrix() {
		return normalMatrix;
	}

	@Override
	public void computeLighting(MutableQuadViewImpl quad) {
		// UGLY: for vanilla lighting need to undo diffuse shading
		ColorHelper.applyDiffuseShading(quad, true);
	}

	@Override
	public void applyLighting(MutableQuadViewImpl quad) {
		final int lightmap = quad.material().emissive(0) ? VertexEncoder.FULL_BRIGHTNESS : ItemRenderContext.this.lightmap;

		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmap));
		}
	}

	@Override
	public VertexConsumer consumer(MutableQuadViewImpl quad) {
		return quadVertexConsumer(quad.material().blendMode(0));
	}

	@Override
	public int indexedColor(int colorIndex) {
		return colorIndex == -1 ? -1 : (colorMap.getColorMultiplier(itemStack, colorIndex) | 0xFF000000);
	}
}
