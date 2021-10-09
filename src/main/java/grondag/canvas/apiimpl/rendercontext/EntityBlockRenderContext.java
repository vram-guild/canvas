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

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.model.BlockModel;
import io.vram.sc.concurrency.SimpleConcurrentList;

import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.render.world.CanvasWorldRenderer;

/**
 * Context used when blocks are rendered as part of an entity.
 * Vanilla examples include blocks held be endermen, blocks in minecarts,
 * flowers held by iron golems and Mooshroom mushrooms.
 *
 * <p>Also handle rendering of the item frame which looks and acts like a block
 * and has a block JSON model but is an entity.
 */
public class EntityBlockRenderContext extends AbstractBlockRenderContext<BlockAndTintGetter> {
	private static final SimpleConcurrentList<AbstractRenderContext> LOADED = new SimpleConcurrentList<>(AbstractRenderContext.class);

	private static final Supplier<ThreadLocal<EntityBlockRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final EntityBlockRenderContext result = new EntityBlockRenderContext();
		LOADED.add(result);
		return result;
	});

	private static ThreadLocal<EntityBlockRenderContext> POOL = POOL_FACTORY.get();

	private int light;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private float tickDelta;

	public EntityBlockRenderContext() {
		super("BlockRenderContext");
	}

	public static void reload() {
		LOADED.forEach(c -> c.close());
		LOADED.clear();
		POOL = POOL_FACTORY.get();
	}

	public static EntityBlockRenderContext get() {
		return POOL.get();
	}

	public void tickDelta(float tickDelta) {
		this.tickDelta = tickDelta;
	}

	public void setPosAndWorldFromEntity(Entity entity) {
		if (entity != null) {
			final float tickDelta = this.tickDelta;
			final double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
			final double y = Mth.lerp(tickDelta, entity.yo, entity.getY()) + entity.getEyeHeight();
			final double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
			pos.set(x, y, z);
			region = entity.getCommandSenderWorld();
		}
	}

	/**
	 * Assumes region and block pos set earlier via {@link #setPosAndWorldFromEntity(Entity)}.
	 */
	@SuppressWarnings("resource")
	public void render(ModelBlockRenderer vanillaRenderer, BakedModel model, BlockState state, PoseStack matrixStack, MultiBufferSource consumers, int overlay, int light) {
		defaultConsumer = consumers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false));
		matrix = matrixStack.last().pose();
		normalMatrix = (Matrix3fExt) (Object) matrixStack.last().normal();
		this.light = light;
		this.overlay = overlay;
		region = CanvasWorldRenderer.instance().worldRenderState.getWorld();
		prepareForBlock(state, pos, model.useAmbientOcclusion(), 42);
		((BlockModel) model).renderAsBlock(this, this);
		defaultConsumer = null;
	}

	// item frames don't have a block state but render like a block
	@SuppressWarnings("resource")
	public void renderItemFrame(ModelBlockRenderer modelRenderer, BakedModel model, PoseStack matrixStack, MultiBufferSource consumers, int overlay, int light, ItemFrame itemFrameEntity) {
		defaultConsumer = consumers.getBuffer(Sheets.solidBlockSheet());
		matrix = matrixStack.last().pose();
		normalMatrix = (Matrix3fExt) (Object) matrixStack.last().normal();
		this.light = light;
		this.overlay = overlay;
		region = CanvasWorldRenderer.instance().worldRenderState.getWorld();

		pos.set(itemFrameEntity.getX(), itemFrameEntity.getY(), itemFrameEntity.getZ());
		blockPos = pos;
		blockState = Blocks.AIR.defaultBlockState();
		materialMap = MaterialMap.defaultMaterialMap();
		lastColorIndex = -1;
		needsRandomRefresh = true;
		fullCubeCache = 0;
		seed = 42;
		defaultAo = false;
		defaultPreset = MaterialConstants.PRESET_SOLID;

		((BlockModel) model).renderAsBlock(this, this);
		defaultConsumer = null;
	}

	@Override
	public int brightness() {
		return light;
	}

	@Override
	protected int fastBrightness(BlockState blockState, BlockPos pos) {
		return light;
	}

	@Override
	protected void adjustMaterial() {
		super.adjustMaterial();
		finder.disableAo(true);
	}

	@Override
	public void computeAo(QuadEditorImpl quad) {
		// NOOP
	}

	@Override
	public void computeFlat(QuadEditorImpl quad) {
		computeFlatSimple(quad);
	}
}
