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

package grondag.canvas.apiimpl.rendercontext;

import static io.vram.frex.base.renderer.util.EncoderUtil.applyItemLighting;
import static io.vram.frex.base.renderer.util.EncoderUtil.colorizeQuad;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractBannerBlock;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.model.ItemModel;
import io.vram.frex.api.rendertype.VanillaShaderInfo;
import io.vram.frex.base.renderer.context.BaseItemContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;
import io.vram.frex.base.renderer.util.EncoderUtil;

import grondag.canvas.buffer.format.StandardEncoder;
import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderContextState.GuiMode;
import grondag.canvas.mixinterface.ItemRendererExt;

public class ItemRenderContext extends AbstractRenderContext<BaseItemContext> {
	private static final Supplier<ThreadLocal<ItemRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final ItemRenderContext result = new ItemRenderContext();
		return result;
	});

	private static ThreadLocal<ItemRenderContext> POOL = POOL_FACTORY.get();

	protected int lightmap;

	private RenderType defaultRenderLayer;
	private VertexConsumer defaultConsumer;

	public ItemRenderContext() {
		super("ItemRenderContext");
	}

	@Override
	protected BaseItemContext createInputContext() {
		return new BaseItemContext() {
			@Override
			public int flatBrightness(BaseQuadEmitter quad) {
				return lightmap;
			}
		};
	}

	public static void reload() {
		POOL = POOL_FACTORY.get();
	}

	public static ItemRenderContext get() {
		return POOL.get();
	}

	@Override
	public boolean defaultAo() {
		return false;
	}

	@Override
	public int brightness() {
		return lightmap;
	}

	/**
	 * True when drawing to GUI or first person perspective.
	 */
	private boolean drawTranslucencyDirectToMainTarget;

	/**
	 * When false, assume item models are generated and should be rendered with cutout enabled if blend mode is translucent.
	 * This prevents
	 */
	private boolean isBlockItem;

	private boolean isGui;

	/**
	 * True for generated models when in GUI and diffuse shading shouldn't be used.
	 * True only when isGui is true;
	 */
	private boolean isFrontLit;

	private boolean hasGlint;

	public void renderItem(ItemModelShaper models, ItemStack stack, TransformType renderMode, boolean leftHanded, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, int overlay, BakedModel model) {
		if (stack.isEmpty()) return;

		lightmap = light;
		final MatrixStack matrixStack = MatrixStack.cast(poseStack);
		inputContext.prepareForItem(stack, renderMode, overlay, matrixStack);
		isBlockItem = stack.getItem() instanceof BlockItem;
		materialMap = MaterialMap.get(stack);
		isGui = renderMode == ItemTransforms.TransformType.GUI;
		isFrontLit = isGui && !model.usesBlockLight();
		hasGlint = stack.hasFoil();
		matrixStack.push();
		final boolean detachedPerspective = renderMode == ItemTransforms.TransformType.GUI || renderMode == ItemTransforms.TransformType.GROUND || renderMode == ItemTransforms.TransformType.FIXED;

		if (detachedPerspective) {
			if (stack.is(Items.TRIDENT)) {
				model = models.getItemModel(Items.TRIDENT);
			} else if (stack.is(Items.SPYGLASS)) {
				model = models.getItemModel(Items.SPYGLASS);
			}
		}

		// PERF: optimize matrix stack operations
		model.getTransforms().getTransform(renderMode).apply(leftHanded, poseStack);
		matrixStack.translate(-0.5f, -0.5f, -0.5f);

		if (model.isCustomRenderer() || stack.getItem() == Items.TRIDENT && !detachedPerspective) {
			final BlockEntityWithoutLevelRenderer builtInRenderer = ((ItemRendererExt) Minecraft.getInstance().getItemRenderer()).canvas_builtinModelItemRenderer();

			if (isGui && vertexConsumers instanceof CanvasImmediate) {
				final RenderContextState context = ((CanvasImmediate) vertexConsumers).contextState;
				context.guiMode(isBlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof AbstractBannerBlock ? GuiMode.GUI_FRONT_LIT : GuiMode.NORMAL);
				builtInRenderer.renderByItem(stack, renderMode, poseStack, vertexConsumers, light, overlay);
				context.guiMode(GuiMode.NORMAL);
			} else {
				builtInRenderer.renderByItem(stack, renderMode, poseStack, vertexConsumers, light, overlay);
			}
		} else {
			drawTranslucencyDirectToMainTarget = isGui || renderMode.firstPerson() || !isBlockItem;
			defaultRenderLayer = ItemBlockRenderTypes.getRenderType(stack, drawTranslucencyDirectToMainTarget);
			defaultPreset = inferDefaultItemPreset(defaultRenderLayer);

			if (((vertexConsumers instanceof CanvasImmediate))) {
				collectors = ((CanvasImmediate) vertexConsumers).collectors;
			} else {
				collectors = null;
				defaultConsumer = vertexConsumers.getBuffer(defaultRenderLayer);
			}

			((ItemModel) model).renderAsItem(inputContext, emitter());
		}

		matrixStack.pop();
	}

	@Override
	protected void adjustMaterial() {
		final MaterialFinder finder = this.finder;

		finder.foilOverlay(hasGlint);

		if (inputContext.overlay() != OverlayTexture.NO_OVERLAY) {
			finder.overlay(inputContext.overlay());
		}

		int preset = finder.preset();

		// fully specific renderable material
		if (preset == MaterialConstants.PRESET_NONE) return;

		if (preset == MaterialConstants.PRESET_DEFAULT) {
			preset = defaultPreset;
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
					.cutout(isBlockItem ? MaterialConstants.CUTOUT_NONE : MaterialConstants.CUTOUT_TENTH)
					.unmipped(false)
					.target(drawTranslucencyDirectToMainTarget ? MaterialConstants.TARGET_MAIN : MaterialConstants.TARGET_ENTITIES)
					.sorted(!drawTranslucencyDirectToMainTarget);
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

		if (isGui && isFrontLit) {
			finder.disableDiffuse(true);
		}

		finder.disableAo(true);
	}

	@Override
	public void computeAo(BaseQuadEmitter quad) {
		// NOOP
	}

	@Override
	public void computeFlat(BaseQuadEmitter quad) {
		computeFlatSimple(quad);
	}

	@Override
	protected void encodeQuad(BaseQuadEmitter quad) {
		colorizeQuad(quad, this.inputContext);
		applyItemLighting(quad, this.lightmap);

		if (collectors == null) {
			EncoderUtil.encodeQuad(quad, inputContext, defaultConsumer);
		} else {
			StandardEncoder.encodeQuad(quad, inputContext, collectors.get((CanvasRenderMaterial) quad.material()));
		}
	}

	static int inferDefaultItemPreset(RenderType layer) {
		final var compositeState = ((CompositeRenderType) layer).state;

		if (compositeState.transparencyState == RenderStateShard.TRANSLUCENT_TRANSPARENCY) {
			return MaterialConstants.PRESET_TRANSLUCENT;
		} else if (VanillaShaderInfo.get(compositeState.shaderState).cutout() != MaterialConstants.CUTOUT_NONE) {
			final TextureStateShard tex = (TextureStateShard) compositeState.textureState;
			return tex.mipmap ? MaterialConstants.PRESET_CUTOUT_MIPPED : MaterialConstants.PRESET_CUTOUT;
		} else {
			return MaterialConstants.PRESET_SOLID;
		}
	}
}
