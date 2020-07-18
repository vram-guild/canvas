package grondag.canvas.apiimpl.rendercontext;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.buffer.encoding.VertexEncoder;
import grondag.canvas.mixinterface.RenderLayerExt;
import grondag.frex.api.material.MaterialMap;

public abstract class AbstractBlockRenderContext<T extends BlockRenderView > extends AbstractRenderContext implements RenderContext {
	/** for use by chunk builder - avoids another threadlocal */
	public final BlockPos.Mutable searchPos = new BlockPos.Mutable();

	/** for internal use */
	protected final BlockPos.Mutable internalSearchPos = new BlockPos.Mutable();

	private final BlockColors blockColorMap = MinecraftClient.getInstance().getBlockColors();
	private boolean needsRandomRefresh = true;
	private int lastColorIndex = -1;
	private int blockColor = -1;
	private int fullCubeCache = 0;

	public final Random random = new Random();
	public T region;
	public BlockPos blockPos;
	public BlockState blockState;
	public long seed;
	public boolean defaultAo;
	public int defaultBlendModeIndex;


	public final Supplier<Random> randomSupplier = () -> {
		final Random result = random;

		if(needsRandomRefresh) {
			needsRandomRefresh = false;
			long seed = this.seed;

			if (seed == -1L) {
				seed = blockState.getRenderingSeed(blockPos);
				this.seed = seed;
			}

			result.setSeed(seed);
		}

		return result;
	};

	/**
	 *
	 * @param blockState
	 * @param blockPos
	 * @param modelAO
	 * @param seed pass -1 for default behavior
	 */
	public void prepareForBlock(BlockState blockState, BlockPos blockPos, boolean modelAO, long seed) {
		this.blockPos = blockPos;
		this.blockState = blockState;

		materialMap = MaterialMap.get(blockState);
		lastColorIndex = -1;
		needsRandomRefresh = true;
		fullCubeCache = 0;
		this.seed = seed;
		defaultAo = modelAO && MinecraftClient.isAmbientOcclusionEnabled() && blockState.getLuminance() == 0;
		defaultBlendModeIndex = ((RenderLayerExt) RenderLayers.getBlockLayer(blockState)).canvas_blendModeIndex();
	}

	@Override
	public final int indexedColor(int colorIndex) {
		if(colorIndex == -1) {
			return -1;
		} else if(lastColorIndex == colorIndex) {
			return blockColor;
		} else {
			lastColorIndex = colorIndex;
			final int result = 0xFF000000 | blockColorMap.getColor(blockState, region, blockPos, colorIndex);
			blockColor = result;
			return result;
		}
	}

	public boolean isFullCube() {
		if (fullCubeCache == 0) {
			fullCubeCache = Block.isShapeFullCube(blockState.getCollisionShape(region, blockPos)) ? 1 : -1;
		}

		return fullCubeCache == 1;
	}

	@Override
	public final Random random() {
		return randomSupplier.get();
	}

	@Override
	public final boolean defaultAo() {
		return defaultAo;
	}

	@Override
	public final BlockState blockState() {
		return blockState;
	}

	@Override
	public final int flatBrightness(MutableQuadViewImpl quad) {
		/**
		 * Handles geometry-based check for using self brightness or neighbor brightness.
		 * That logic only applies in flat lighting.
		 */
		if (blockState.hasEmissiveLighting(region, blockPos)) {
			return VertexEncoder.FULL_BRIGHTNESS;
		}

		internalSearchPos.set(blockPos);

		// To mirror Vanilla's behavior, if the face has a cull-face, always sample the light value
		// offset in that direction. See net.minecraft.client.render.block.BlockModelRenderer.renderFlat
		// for reference.
		if (quad.cullFaceId() != ModelHelper.NULL_FACE_ID) {
			internalSearchPos.move(quad.cullFace());
		} else if ((quad.geometryFlags() & GeometryHelper.LIGHT_FACE_FLAG) != 0 || isFullCube()) {
			internalSearchPos.move(quad.lightFace());
		}

		return fastBrightness(blockState, internalSearchPos);
	}

	@Override
	protected int defaultBlendModeIndex() {
		return defaultBlendModeIndex;
	}

	protected abstract int fastBrightness(BlockState blockState, BlockPos pos);
}
