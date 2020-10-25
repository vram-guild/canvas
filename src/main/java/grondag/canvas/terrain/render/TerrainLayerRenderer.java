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

package grondag.canvas.terrain.render;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderPass;
import grondag.canvas.terrain.BuiltRenderRegion;
import grondag.canvas.terrain.TerrainModelSpace;
import grondag.canvas.texture.DitherTexture;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

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

		final int startIndex = isTranslucent ? visibleRegionCount - 1 : 0;
		final int endIndex = isTranslucent ? -1 : visibleRegionCount;
		final int step = isTranslucent ? -1 : 1;
		final ShaderPass pass = shaderContext.pass;

		if (Configurator.hdLightmaps()) {
			LightmapHdTexture.instance().enable();
			DitherTexture.instance().enable();
		}

		long lastRelativeOrigin = -1;

		//		final DrawHandler h = DrawHandlers.get(EncodingContext.TERRAIN, shaderContext.pass);
		//		final MaterialVertexFormat format = h.format;
		//		h.setup();

		int ox = 0, oy = 0, oz = 0;

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

							ox = TerrainModelSpace.renderCubeOrigin(modelOrigin.getX());
							oy = TerrainModelSpace.renderCubeOrigin(modelOrigin.getY());
							oz = TerrainModelSpace.renderCubeOrigin(modelOrigin.getZ());

							matrixStack.push();
							matrixStack.translate(ox - x, oy - y, oz - z);
							RenderSystem.pushMatrix();
							RenderSystem.loadIdentity();
							RenderSystem.multMatrix(matrixStack.peek().getModel());
						}
					} else {
						ox = modelOrigin.getX();
						oy = modelOrigin.getY();
						oz = modelOrigin.getZ();

						matrixStack.push();
						matrixStack.translate(ox - x, oy - y, oz - z);
						RenderSystem.pushMatrix();
						RenderSystem.loadIdentity();
						RenderSystem.multMatrix(matrixStack.peek().getModel());
					}



					drawable.vboBuffer.bind();

					final int limit = delegates.size();

					for (int i = 0; i < limit; ++i) {
						final DrawableDelegate d = delegates.get(i);
						d.materialState().renderState.enable();

						final MaterialConditionImpl condition = d.materialState().condition;

						if (!condition.affectBlocks || condition.compute()) {
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
