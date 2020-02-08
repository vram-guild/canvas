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
package grondag.canvas.mixin;

import java.util.Set;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.chunk.TerrainRenderer;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.perf.MicroTimer;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer implements WorldRendererExt {
	@Shadow private MinecraftClient client;
	@Shadow private int renderDistance;
	@Shadow private ClientWorld world;
	@Shadow private double lastCameraChunkUpdateX;
	@Shadow private double lastCameraChunkUpdateY;
	@Shadow private double lastCameraChunkUpdateZ;
	@Shadow private int cameraChunkX;
	@Shadow private int cameraChunkY;
	@Shadow private int cameraChunkZ;
	@Shadow private double lastCameraX;
	@Shadow private double lastCameraY;
	@Shadow private double lastCameraZ;
	@Shadow private double lastCameraPitch;
	@Shadow private double lastCameraYaw;
	@Shadow private ChunkBuilder chunkBuilder;
	@Shadow private Set<ChunkBuilder.BuiltChunk> chunksToRebuild;
	@SuppressWarnings("rawtypes")
	@Shadow private ObjectList visibleChunks;
	@Shadow private BuiltChunkStorage chunks;
	@Shadow private boolean needsTerrainUpdate;

	private final TerrainRenderer terrainRenderer = new TerrainRenderer(this);

	// TODO: remove
	private static final MicroTimer timer = new MicroTimer("setupTerrain", 200);

	@Inject(at = @At("HEAD"), method = "setupTerrain", cancellable = true)
	private void onSetupTerrain(Camera camera, Frustum frustum, boolean bl, int i, boolean bl2, CallbackInfo ci) {
		timer.start();
		terrainRenderer.setupTerrain(camera, frustum, bl, i, bl2);
		timer.stop();
		ci.cancel();
	}

	@Override
	public MinecraftClient canvas_mc() {
		return client;
	}

	@Override
	public int canvas_renderDistance() {
		return renderDistance;
	}

	@Override
	public void canvas_reload() {
		((WorldRenderer)(Object) this).reload();
	}

	@Override
	public ClientWorld canvas_world() {
		return world;
	}

	@Override
	public double canvas_lastCameraChunkUpdateX() {
		return lastCameraChunkUpdateX;
	}

	@Override
	public double canvas_lastCameraChunkUpdateY() {
		return lastCameraChunkUpdateY;
	}

	@Override
	public double canvas_lastCameraChunkUpdateZ() {
		return lastCameraChunkUpdateZ;
	}

	@Override
	public void canvas_updateLastCameraChunkPositions() {
		lastCameraChunkUpdateX = client.player.getX();
		lastCameraChunkUpdateY = client.player.getY();
		lastCameraChunkUpdateZ = client.player.getZ();
		cameraChunkX = client.player.chunkX;
		cameraChunkY = client.player.chunkY;
		cameraChunkZ = client.player.chunkZ;
	}

	@Override
	public int canvas_camereChunkX() {
		return cameraChunkX;
	}

	@Override
	public int canvas_camereChunkY() {
		return cameraChunkY;
	}

	@Override
	public int canvas_camereChunkZ() {
		return cameraChunkZ;
	}

	@Override
	public BuiltChunkStorage canvas_chunks() {
		return chunks;
	}

	@Override
	public Set<BuiltChunk> canvas_chunkToRebuild() {
		return chunksToRebuild;
	}

	@Override
	public Set<BuiltChunk> canvas_newChunkToRebuild() {
		chunksToRebuild = Sets.newLinkedHashSet();
		return chunksToRebuild;
	}

	@Override
	public ChunkBuilder canvas_chunkBuilder() {
		return chunkBuilder;
	}

	@Override
	public boolean canvas_checkNeedsTerrainUpdate(Vec3d cameraPos, float pitch, float yaw) {
		needsTerrainUpdate = needsTerrainUpdate || !chunksToRebuild.isEmpty() || cameraPos.x != lastCameraX || cameraPos.y != lastCameraY || cameraPos.z != lastCameraZ || pitch != lastCameraPitch || yaw != lastCameraYaw;
		lastCameraX = cameraPos.x;
		lastCameraY = cameraPos.y;
		lastCameraZ = cameraPos.z;
		lastCameraPitch = pitch;
		lastCameraYaw = yaw;

		return needsTerrainUpdate;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public ObjectList canvas_visibleChunks() {
		return visibleChunks;
	}

	@Override
	public boolean canvas_setNeedsTerrainUpdate(boolean needsUpdate) {
		needsTerrainUpdate = needsUpdate;
		return needsUpdate;
	}
}
