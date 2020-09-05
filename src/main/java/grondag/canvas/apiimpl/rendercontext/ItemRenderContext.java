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

package grondag.canvas.apiimpl.rendercontext;

import grondag.canvas.apiimpl.material.MeshMaterialLayer;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.EncodingContext;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.MinecraftClientExt;
import grondag.fermion.sc.concurrency.SimpleConcurrentList;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation.Mode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import java.util.Random;
import java.util.function.Supplier;

/**
 * The render context used for item rendering.
 * Does not implement emissive lighting for sake
 * of simplicity in the default renderer.
 */
public class ItemRenderContext extends AbstractRenderContext implements RenderContext {
	/**
	 * Value vanilla uses for item rendering.  The only sensible choice, of course.
	 */
	private static final long ITEM_RANDOM_SEED = 42L;

	private static final SimpleConcurrentList<AbstractRenderContext> LOADED = new SimpleConcurrentList<>(AbstractRenderContext.class);

	private static final Supplier<ThreadLocal<ItemRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final ItemRenderContext result = new ItemRenderContext(((MinecraftClientExt) MinecraftClient.getInstance()).canvas_itemColors());
		LOADED.add(result);
		return result;
	});

	private static ThreadLocal<ItemRenderContext> POOL = POOL_FACTORY.get();
	private final ItemColors colorMap;
	private final Random random = new Random();
	private final EncodingContext context = EncodingContext.ITEM;
	private final Supplier<Random> randomSupplier = () -> {
		final Random result = random;
		result.setSeed(ITEM_RANDOM_SEED);
		return random;
	};
	private VertexConsumer modelVertexConsumer;
	private int lightmap;
	private ItemStack itemStack;

	public ItemRenderContext(ItemColors colorMap) {
		super("ItemRenderContext");
		this.colorMap = colorMap;
		collectors.setContext(EncodingContext.ITEM);
	}

	public static void reload() {
		LOADED.forEach(c -> c.close());
		LOADED.clear();
		POOL = POOL_FACTORY.get();
	}

	public static ItemRenderContext get() {
		return POOL.get();
	}

	public void renderModel(ItemStack itemStack, Mode transformMode, boolean invert, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, VertexConsumer modelConsumer, int lightmap, int overlay, FabricBakedModel model) {
		this.lightmap = lightmap;
		this.overlay = overlay;
		this.itemStack = itemStack;
		modelVertexConsumer = modelConsumer;
		matrixStack.push();

		matrix = matrixStack.peek().getModel();
		normalMatrix = (Matrix3fExt) (Object) matrixStack.peek().getNormal();

		model.emitItemQuads(itemStack, randomSupplier, this);

		matrixStack.pop();

		this.itemStack = null;
		modelVertexConsumer = null;
	}


	@Override
	public EncodingContext materialContext() {
		return context;
	}

	@Override
	protected Random random() {
		return randomSupplier.get();
	}

	@Override
	public boolean defaultAo() {
		return false;
	}

	@Override
	public BlockState blockState() {
		return null;
	}

	@Override
	public VertexConsumer consumer(MeshMaterialLayer mat) {
		// WIP: really can't honor per-quad materials in the current setup
		// and also honor default model render layer because default blend mode
		// is transformed to something specific before we get here, and the
		// vanilla logic for model default layer is monstrous - see ItemRenderer
		// For now, always use the model default
		return modelVertexConsumer;
	}

	@Override
	public int indexedColor(int colorIndex) {
		return colorIndex == -1 ? -1 : (colorMap.getColorMultiplier(itemStack, colorIndex) | 0xFF000000);
	}

	@Override
	public int brightness() {
		return lightmap;
	}

	@Override
	public AoCalculator aoCalc() {
		return null;
	}

	@Override
	public int flatBrightness(MutableQuadViewImpl quad) {
		return 0;
	}

	@Override
	protected int defaultBlendModeIndex() {
		return BlendMode.TRANSLUCENT.ordinal();
	}
}
