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

import java.util.Random;
import java.util.function.Supplier;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.encoding.CanvasImmediate;
import grondag.canvas.material.state.MaterialFinderImpl;
import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.MinecraftClientExt;
import grondag.fermion.sc.concurrency.SimpleConcurrentList;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialMap;
import org.jetbrains.annotations.Nullable;

import static grondag.canvas.buffer.encoding.EncoderUtils.applyItemLighting;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuad;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuadDirect;
import static grondag.canvas.buffer.encoding.EncoderUtils.colorizeQuad;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelTransformation.Mode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

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
	private final Supplier<Random> randomSupplier = () -> {
		final Random result = random;
		result.setSeed(ITEM_RANDOM_SEED);
		return random;
	};
	private MatrixStack matrices;
	private Mode renderMode;
	private RenderLayer defaultRenderLayer;
	private VertexConsumerProvider vanillaProvider;
	private VertexConsumer defaultConsumer;
	private @Nullable VertexConsumer glintConsumer;

	private int lightmap;
	private ItemStack itemStack;

	public ItemRenderContext(ItemColors colorMap) {
		super("ItemRenderContext");
		this.colorMap = colorMap;
	}

	public static void reload() {
		LOADED.forEach(c -> c.close());
		LOADED.clear();
		POOL = POOL_FACTORY.get();
	}

	public static ItemRenderContext get() {
		return POOL.get();
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
	public int indexedColor(int colorIndex) {
		return colorIndex == -1 ? -1 : (colorMap.getColorMultiplier(itemStack, colorIndex) | 0xFF000000);
	}

	@Override
	public int brightness() {
		return lightmap;
	}

	@Override
	public int flatBrightness(MutableQuadViewImpl quad) {
		return 0;
	}

	/**
	 * True except for translucent blocks and glass panes or when drawing to GUI or first person perspective.
	 * Even if drawn with translucency, the model can be expected to cull itself and the scene and distance
	 * sorting isn't important.
	 */
	private boolean drawDirectToMainTarget;

	/**
	 * When false, assume item models are generated and should be rendered with cutout enabled if blend mode is translucent.
	 * This prevents
	 */
	private boolean isBlockItem;

	public void renderItem(ItemModels models, ItemStack stack, Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model) {
		if (stack.isEmpty()) return;

		lightmap = light;
		this.overlay = overlay;
		this.matrices = matrices;
		this.renderMode = renderMode;
		itemStack = stack;
		vanillaProvider = vertexConsumers;
		materialMap = MaterialMap.get(itemStack);

		matrices.push();
		final boolean detachedPerspective = renderMode == ModelTransformation.Mode.GUI || renderMode == ModelTransformation.Mode.GROUND || renderMode == ModelTransformation.Mode.FIXED;

		if (stack.getItem() == Items.TRIDENT && detachedPerspective) {
			model = models.getModelManager().getModel(new ModelIdentifier("minecraft:trident#inventory"));
		}

		// PERF: optimize matrix stack operations
		model.getTransformation().getTransformation(renderMode).apply(leftHanded, matrices);
		matrices.translate(-0.5D, -0.5D, -0.5D);

		matrix = matrices.peek().getModel();
		normalMatrix = (Matrix3fExt) (Object) matrices.peek().getNormal();

		if (model.isBuiltin() || stack.getItem() == Items.TRIDENT && !detachedPerspective) {
			isBlockItem = false;
			BuiltinModelItemRenderer.INSTANCE.render(stack, renderMode, matrices, vertexConsumers, light, overlay);
		} else {
			isBlockItem = stack.getItem() instanceof BlockItem;

			if (renderMode != ModelTransformation.Mode.GUI && !renderMode.isFirstPerson() && isBlockItem) {
				final Block block = ((BlockItem)stack.getItem()).getBlock();
				// WIP: this probably doesn't work for modded blocks that have translucent quads (like XB)
				// determination needs to happen at quad level.
				drawDirectToMainTarget = !(block instanceof TransparentBlock) && !(block instanceof StainedGlassPaneBlock);
			} else {
				drawDirectToMainTarget = true;
			}

			defaultRenderLayer = RenderLayers.getItemLayer(stack, drawDirectToMainTarget);
			glintConsumer = getGlintConsumer(defaultRenderLayer);
			defaultBlendMode = RenderLayerHelper.blendModeFromLayer(defaultRenderLayer);

			if (((vertexConsumers instanceof CanvasImmediate))) {
				collectors = ((CanvasImmediate) vertexConsumers).collectors;
			} else {
				collectors = null;
				defaultConsumer = vertexConsumers.getBuffer(defaultRenderLayer);
			}

			((FabricBakedModel) model).emitItemQuads(itemStack, randomSupplier, this);
		}

		matrices.pop();

	}

	private VertexConsumer getGlintConsumer(RenderLayer layer) {
		if (!itemStack.hasGlint()) {
			return null;
		}

		if (itemStack.getItem() == Items.COMPASS) {
			// WTAF MOJANG
			final VertexConsumer result;
			matrices.push();
			final MatrixStack.Entry entry = matrices.peek();

			if (renderMode == ModelTransformation.Mode.GUI) {
				entry.getModel().multiply(0.5F);
			} else if (renderMode.isFirstPerson()) {
				entry.getModel().multiply(0.75F);
			}

			if (drawDirectToMainTarget) {
				result = getDirectCompassGlintConsumer(vanillaProvider, layer, entry);
			} else {
				result = getCompassGlintConsumer(vanillaProvider, layer, entry);
			}

			matrices.pop();
			return result;
		} else if (drawDirectToMainTarget) {
			return getDirectItemGlintConsumer(vanillaProvider, layer);
		} else {
			return getItemGlintConsumer(vanillaProvider, layer);
		}
	}

	@Override
	protected void adjustMaterial() {
		final MaterialFinderImpl finder = this.finder;

		BlendMode bm = finder.blendMode();

		// fully specific renderable material
		if (bm == null) return;

		if (finder.blendMode() == BlendMode.DEFAULT) {
			bm = defaultBlendMode;
			finder.blendMode(null);
		}

		switch (bm) {
			case CUTOUT:
				finder.transparency(MaterialFinder.TRANSPARENCY_NONE)
				.cutout(true)
				.transparentCutout(false)
				.unmipped(true)
				.target(MaterialFinder.TARGET_MAIN)
				.sorted(false);
				break;
			case CUTOUT_MIPPED:
				finder.transparency(MaterialFinder.TRANSPARENCY_NONE)
				.cutout(true)
				.transparentCutout(false)
				.unmipped(false)
				.target(MaterialFinder.TARGET_MAIN)
				.sorted(false);
				break;
			case TRANSLUCENT:
				// Note on glint rendering
				// Glint renders use EQUALS depth test.
				// This makes it important that
				//   1) geometry is the same
				//   2) cutout is enabled for generated models so depth buffer isn't updated
				// 1 is easily solved by renderin twice with same vertex data
				// 2 has to be finessed because blend mode = TRANSLUCENT doesn't make it clear cutout is needed.
				// The code below is an ugly hack - need a better way

				finder.transparency(MaterialFinder.TRANSPARENCY_TRANSLUCENT)
				.cutout(!isBlockItem)
				.transparentCutout(!isBlockItem)
				.unmipped(false)
				.target(drawDirectToMainTarget ? MaterialFinder.TARGET_MAIN : MaterialFinder.TARGET_ENTITIES)
				.sorted(true);
				break;
			case SOLID:
				finder.transparency(MaterialFinder.TRANSPARENCY_NONE)
				.cutout(false)
				.transparentCutout(false)
				.unmipped(false)
				.target(MaterialFinder.TARGET_MAIN)
				.sorted(false);
				break;
			default:
				assert false : "Unhandled blend mode";
		}

		finder.disableAo(true);
	}

	@Override
	public void computeAo(MutableQuadViewImpl quad) {
		// NOOP
	}

	@Override
	public void computeFlat(MutableQuadViewImpl quad) {
		computeFlatSimple(quad);
	}

	@Override
	protected void encodeQuad(MutableQuadViewImpl quad) {
		colorizeQuad(quad, this);
		applyItemLighting(quad, this);

		if (collectors == null) {
			bufferQuad(quad, this, defaultConsumer);
		} else {
			bufferQuadDirect(quad, this, collectors.get(quad.material()));
		}

		if (glintConsumer != null) {
			bufferQuad(quad, this, glintConsumer);
		}
	}

	// differ from vanilla in that aren't dual - just render quads twice
	// PERF: avoid reallocation
	private static VertexConsumer getCompassGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, MatrixStack.Entry entry) {
		return new OverlayVertexConsumer(provider.getBuffer(RenderLayer.getGlint()), entry.getModel(), entry.getNormal());
	}

	private static VertexConsumer getDirectCompassGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, MatrixStack.Entry entry) {
		return new OverlayVertexConsumer(provider.getBuffer(RenderLayer.getDirectGlint()), entry.getModel(), entry.getNormal());
	}

	private static VertexConsumer getItemGlintConsumer(VertexConsumerProvider vertexConsumers, RenderLayer layer) {
		return MinecraftClient.isFabulousGraphicsOrBetter() && layer == TexturedRenderLayers.getItemEntityTranslucentCull() ? vertexConsumers.getBuffer(RenderLayer.method_30676()) : vertexConsumers.getBuffer(RenderLayer.getGlint());
	}

	private static VertexConsumer getDirectItemGlintConsumer(VertexConsumerProvider provider, RenderLayer layer) {
		return provider.getBuffer(RenderLayer.getDirectGlint());
	}
}
