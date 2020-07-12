package grondag.canvas.terrain.render;

import javax.annotation.Nullable;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.render.DrawHandler;
import grondag.canvas.render.DrawHandlers;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderPass;
import grondag.canvas.terrain.BuiltRenderRegion;
import grondag.canvas.terrain.TerrainModelSpace;
import grondag.canvas.texture.DitherTexture;

public class TerrainLayerRenderer {
	private final String profileString;
	private final Runnable sortTask;
	private final boolean isTranslucent;
	private final ShaderContext shaderContext;

	public TerrainLayerRenderer(String layerName, ShaderContext shaderContext, @Nullable Runnable translucentSortTask) {
		profileString = "render_" + layerName;
		this.shaderContext = shaderContext;
		isTranslucent = translucentSortTask != null;
		sortTask = isTranslucent ? translucentSortTask : Runnables.doNothing();

		assert !isTranslucent || shaderContext == ShaderContext.TERRAIN_TRANSLUCENT;
	}

	public void render(final BuiltRenderRegion[] visibleRegions, final int visibleRegionCount, MatrixStack matrixStack, double x, double y, double z) {
		final MinecraftClient mc = MinecraftClient.getInstance();

		sortTask.run();

		mc.getProfiler().push(profileString);

		final int startIndex = isTranslucent ? visibleRegionCount - 1 : 0 ;
		final int endIndex = isTranslucent ? -1 : visibleRegionCount;
		final int step = isTranslucent ? -1 : 1;
		final int frameIndex = MaterialShaderManager.INSTANCE.frameIndex();
		final ShaderPass pass = shaderContext.pass;

		if (Configurator.hdLightmaps()) {
			LightmapHdTexture.instance().enable();
			DitherTexture.instance().enable();
		}

		long lastRelativeOrigin = -1;

		final DrawHandler h = DrawHandlers.get(MaterialContext.TERRAIN, shaderContext.pass);
		final MaterialVertexFormat format = h.format;
		h.setup();

		for (int regionIndex = startIndex; regionIndex != endIndex; regionIndex += step) {
			final BuiltRenderRegion builtRegion = visibleRegions[regionIndex];

			if (builtRegion == null) {
				continue;
			}

			final DrawableChunk drawable = isTranslucent ? builtRegion.translucentDrawable() : builtRegion.solidDrawable();

			if (!drawable.isClosed()) {
				final ObjectArrayList<DrawableDelegate> delegates = drawable.delegates(pass);

				if (delegates != null) {
					final BlockPos modelOrigin = builtRegion.getOrigin();

					if (Configurator.batchedChunkRender) {
						final long newRelativeOrigin = TerrainModelSpace.getPackedOrigin(modelOrigin);

						if (newRelativeOrigin != lastRelativeOrigin) {

							if (lastRelativeOrigin != -1) {
								RenderSystem.popMatrix();
								matrixStack.pop();
							}

							lastRelativeOrigin = newRelativeOrigin;
							matrixStack.push();
							matrixStack.translate(
									TerrainModelSpace.renderCubeOrigin(modelOrigin.getX()) - x,
									TerrainModelSpace.renderCubeOrigin(modelOrigin.getY()) - y,
									TerrainModelSpace.renderCubeOrigin(modelOrigin.getZ()) - z);
							RenderSystem.pushMatrix();
							RenderSystem.loadIdentity();
							RenderSystem.multMatrix(matrixStack.peek().getModel());
						}
					} else {
						matrixStack.push();
						matrixStack.translate(modelOrigin.getX() - x, modelOrigin.getY() - y, modelOrigin.getZ() - z);
						RenderSystem.pushMatrix();
						RenderSystem.loadIdentity();
						RenderSystem.multMatrix(matrixStack.peek().getModel());
					}

					drawable.vboBuffer.bind();

					final int limit = delegates.size();

					for(int i = 0; i < limit; ++i) {
						final DrawableDelegate d = delegates.get(i);
						final MaterialConditionImpl condition = d.materialState().condition;

						if(!condition.affectBlocks || condition.compute(frameIndex)) {
							d.materialState().shader.activate(shaderContext, format);
							d.draw();
						}
					}

					if (!Configurator.batchedChunkRender) {
						RenderSystem.popMatrix();
						matrixStack.pop();
					}
				}
			}
		}

		if (lastRelativeOrigin != -1) {
			RenderSystem.popMatrix();
			matrixStack.pop();
		}

		mc.getProfiler().pop();
	}
}
