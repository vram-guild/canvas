package grondag.canvas.render;

import javax.annotation.Nullable;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.chunk.BuiltRenderRegion;
import grondag.canvas.chunk.DrawableChunk;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.draw.DrawHandler;
import grondag.canvas.draw.DrawHandlers;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderManager;
import grondag.canvas.shader.ShaderPass;

class TerrainLayerRenderer {
	private final String profileString;
	private final Runnable sortTask;
	private final boolean isTranslucent;
	private final ShaderContext shaderContext;

	TerrainLayerRenderer(String layerName, ShaderContext shaderContext, @Nullable Runnable translucentSortTask) {
		profileString = "render_" + layerName;
		this.shaderContext = shaderContext;
		isTranslucent = translucentSortTask != null;
		sortTask = isTranslucent ? translucentSortTask : Runnables.doNothing();

		assert !isTranslucent || shaderContext == ShaderContext.TERRAIN_TRANSLUCENT;
	}

	void render(final BuiltRenderRegion[] visibleRegions, final int visibleRegionCount, MatrixStack matrixStack, double x, double y, double z) {
		final MinecraftClient mc = MinecraftClient.getInstance();

		sortTask.run();

		mc.getProfiler().push(profileString);

		final int startIndex = isTranslucent ? visibleRegionCount - 1 : 0 ;
		final int endIndex = isTranslucent ? -1 : visibleRegionCount;
		final int step = isTranslucent ? -1 : 1;
		final int frameIndex = ShaderManager.INSTANCE.frameIndex();
		final ShaderPass pass = shaderContext.pass;

		if (Configurator.hdLightmaps()) {
			LightmapHdTexture.instance().enable();
		}

		final DrawHandler h = DrawHandlers.get(MaterialContext.TERRAIN, shaderContext.pass);
		final MaterialVertexFormat format = h.format;
		h.setup();

		for (int regionIndex = startIndex; regionIndex != endIndex; regionIndex += step) {
			final BuiltRenderRegion builtRegion = visibleRegions[regionIndex];

			if (builtRegion == null) {
				continue;
			}

			final DrawableChunk drawable = isTranslucent ? builtRegion.translucentDrawable() : builtRegion.solidDrawable();

			if (drawable != null && !drawable.isClosed()) {
				final ObjectArrayList<DrawableDelegate> delegates = drawable.delegates(pass);

				if (delegates != null) {
					matrixStack.push();
					final BlockPos blockPos = builtRegion.getOrigin();
					matrixStack.translate(blockPos.getX() - x, blockPos.getY() - y, blockPos.getZ() - z);
					RenderSystem.pushMatrix();
					RenderSystem.loadIdentity();
					RenderSystem.multMatrix(matrixStack.peek().getModel());
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

					RenderSystem.popMatrix();
					matrixStack.pop();
				}
			}
		}

		mc.getProfiler().pop();
	}
}
