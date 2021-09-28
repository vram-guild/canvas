/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.mixinterface;

import java.util.Set;
import java.util.SortedSet;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RunningTrimmedMean;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface LevelRendererExt {
	void canvas_reload();

	ClientLevel canvas_world();

	TextureManager canvas_textureManager();

	EntityRenderDispatcher canvas_entityRenderDispatcher();

	RenderBuffers canvas_bufferBuilders();

	int canvas_getAndIncrementFrameIndex();

	RunningTrimmedMean canvas_chunkUpdateSmoother();

	boolean canvas_canDrawEntityOutlines();

	RenderTarget canvas_entityOutlinesFramebuffer();

	PostChain canvas_entityOutlineShader();

	Set<BlockEntity> canvas_noCullingBlockEntities();

	void canvas_drawBlockOutline(PoseStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState);

	void canvas_renderWorldBorder(Camera camera);

	Long2ObjectMap<SortedSet<BlockDestructionProgress>> canvas_blockBreakingProgressions();

	void canvas_renderEntity(Entity entity, double d, double e, double f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider);

	void canvas_renderWeather(LightTexture lightmapTextureManager, float f, double d, double e, double g);

	void canvas_setEntityCounts(int regularEntityCount, int blockEntityCount);

	void canvas_setWorldNoSideEffects(ClientLevel world);

	void canvas_setupFabulousBuffers();
}
