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
package grondag.canvas.mixinterface;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface WorldRendererExt {

	MinecraftClient canvas_mc();

	int canvas_renderDistance();

	void canvas_reload();

	ClientWorld canvas_world();

	double canvas_lastCameraChunkUpdateX();

	double canvas_lastCameraChunkUpdateY();

	double canvas_lastCameraChunkUpdateZ();

	void canvas_updateLastCameraChunkPositions();

	int canvas_camereChunkX();

	int canvas_camereChunkY();

	int canvas_camereChunkZ();

	BuiltChunkStorage canvas_chunks();

	Set<BuiltChunk> canvas_chunkToRebuild();

	Set<BuiltChunk> canvas_newChunkToRebuild();

	ChunkBuilder canvas_chunkBuilder();

	/** Updates stored values
	 * @param yaw
	 * @param pitch
	 * @param cameraPos */
	boolean canvas_checkNeedsTerrainUpdate(Vec3d cameraPos, float pitch, float yaw);

	/** Returns value passed in */
	boolean canvas_setNeedsTerrainUpdate(boolean needsUpdate);

	@SuppressWarnings("rawtypes")
	ObjectList canvas_visibleChunks();

	default BuiltChunk canvas_getRenderedChunk(BlockPos pos) {
		return ((BuiltChunkStorageExt) canvas_chunks()).canvas_getRendereredChunk(pos);
	}
}
