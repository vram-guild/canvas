package grondag.canvas.render;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.buffer.allocation.BindStateManager;
import grondag.canvas.buffer.allocation.VboBuffer;
import grondag.canvas.chunk.BuiltRenderRegion;
import grondag.canvas.chunk.DrawableChunk;
import grondag.canvas.chunk.RenderRegionBuilder;
import grondag.canvas.chunk.RenderRegionStorage;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.chunk.occlusion.TerrainOccluder;
import grondag.canvas.chunk.occlusion.region.OcclusionRegion;
import grondag.canvas.chunk.occlusion.region.PackedBox;
import grondag.canvas.draw.DrawHandler;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.ShaderManager;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.Option;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.TransformingVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

public class CanvasWorldRenderer {
	private static CanvasWorldRenderer instance;

	public static CanvasWorldRenderer instance() {
		return instance;
	}

	private boolean terrainSetupOffThread = Configurator.terrainSetupOffThread;
	private int playerLightmap = 0;
	private RenderRegionBuilder regionBuilder;
	private RenderRegionStorage renderRegionStorage;
	private final TerrainIterator terrainIterator = new TerrainIterator();
	private final CanvasFrustum frustum = new CanvasFrustum();
	private int translucentSortPositionVersion;
	private int viewVersion;
	private int occluderVersion;

	/**
	 * Set true when regions have been loaded or determined to be empty and visibility search can progress
	 * or visibility might be changed. Distinct from occluder state, which indiciates if/when occluder must
	 * be reset or redrawn.
	 */
	private boolean shouldUpdateVisibility = false;

	// TODO: redirect uses in MC WorldRenderer
	public final Set<BuiltRenderRegion> regionsToRebuild = Sets.newLinkedHashSet();

	private BuiltRenderRegion[] visibleRegions = new BuiltRenderRegion[4096];
	private int visibleRegionCount = 0;

	private final WorldRendererExt wr;

	public CanvasWorldRenderer(WorldRendererExt wr) {
		this.wr = wr;
		instance = this;
	}

	public void clearRegions() {
		regionsToRebuild.clear();
		regionBuilder.reset();
	}

	public void forceVisibilityUpdate() {
		shouldUpdateVisibility = true;
	}

	@SuppressWarnings("resource")
	public void reload() {
		terrainIterator.reset();
		terrainSetupOffThread = Configurator.terrainSetupOffThread;

		if (regionBuilder == null) {
			regionBuilder = new RenderRegionBuilder(wr.canvas_world(), (WorldRenderer) wr, wr.canvas_mc().is64Bit());
		} else {
			regionBuilder.setWorld(wr.canvas_world());
		}

		if (renderRegionStorage != null) {
			renderRegionStorage.clear();
		}

		clearRegions();
		TerrainOccluder.invalidate();
		renderRegionStorage = new RenderRegionStorage(regionBuilder, wr.canvas_mc().options.viewDistance);
		terrainIterator.setRegionStorage(renderRegionStorage);
		visibleRegionCount = 0;
		visibleRegions = new BuiltRenderRegion[renderRegionStorage.regionCount()];

		final Entity entity = wr.canvas_mc().getCameraEntity();

		if (entity != null) {
			renderRegionStorage.updateRegionOrigins(entity.getX(), entity.getZ());
		}
	}

	public RenderRegionBuilder regionBuilder() {
		return regionBuilder;
	}

	public RenderRegionStorage regionStorage() {
		return renderRegionStorage;
	}

	public boolean isTerrainRenderComplete() {
		return regionsToRebuild.isEmpty() && regionBuilder.isEmpty();
	}

	public void setWorld(@Nullable ClientWorld clientWorld) {
		visibleRegionCount = 0;
		Arrays.fill(visibleRegions, null);
		terrainIterator.reset();
		Arrays.fill(terrainIterator.visibleRegions, null);
	}

	/**
	 PERF:
		unbork main render loop
		add check for visibility to entity shouldRender via Frustum check
		single-draw solid layer via shaders
		render larger cubes - avoid matrix state changes
		cull particle rendering?
		reduce garbage generation
		shared buffers per world column, render order bottom to top
		lod culling: don't render grass, cobwebs, flowers, etc. at longer ranges
		render leaves as solid at distance - omit interior faces
		retain vertex bindings when possible, use VAO


	FIX:
		fix missing edges with off-thread iteration - try frustum check on thread but leave potentially visible set off
	 */

	/**
	 * Terrain rebuild is partly lazy/incremental
	 * The occluder has a thread-safe version indicating visibility test validity.
	 * The raster must be redrawn whenever the frustum view changes but prior visibility
	 * checks remain valid until the player location changes more than 1 block
	 * (regions are fuzzed one block to allow this) or a region that was already drawn into
	 * the raster is updated with different visibility informaiton.  New occluders can
	 * also  be added to the existing raster.
	 * or
	 */
	public void setupTerrain(Camera camera, CanvasFrustum frustum, int frameCounter, boolean isSpectator) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final int renderDistance = wr.canvas_renderDistance();
		final RenderRegionBuilder regionBuilder = this.regionBuilder;
		final RenderRegionStorage regionStorage = renderRegionStorage;
		final BuiltRenderRegion[] regions = regionStorage.regions();
		final TerrainIterator terrainIterator = this.terrainIterator;
		final int frustumPositionVersion = frustum.positionVersion();

		if (mc.options.viewDistance != renderDistance) {
			wr.canvas_reload();
		}

		mc.getProfiler().push("camera");
		final Vec3d cameraPos = camera.getPos();
		regionBuilder.setCameraPosition(cameraPos);
		mc.getProfiler().swap("distance");
		regionStorage.updateCameraDistance(cameraPos, frustumPositionVersion, renderDistance);
		ShaderManager.INSTANCE.prepareForFrame(camera);
		final BlockPos cameraBlockPos = camera.getBlockPos();
		final BuiltRenderRegion cameraRegion = regionStorage.getRegion(cameraBlockPos);

		if (cameraRegion != null)  {
			buildNearRegion(cameraRegion);

			for (final int i : cameraRegion.getNeighborIndices()) {
				if (i != -1) {
					buildNearRegion(regions[i]);
				}
			}
		}

		Entity.setRenderDistanceMultiplier(MathHelper.clamp(mc.options.viewDistance / 8.0D, 1.0D, 2.5D));

		mc.getProfiler().swap("update");

		if (terrainSetupOffThread) {
			int state = terrainIterator.state();

			if (state == TerrainIterator.COMPLETE) {
				final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
				final int size = terrainIterator.visibleRegionCount;
				visibleRegionCount = size;
				System.arraycopy(terrainIterator.visibleRegions, 0, visibleRegions, 0, size);
				assert size == 0 || visibleRegions[0] != null;
				scheduleOrBuild(terrainIterator.updateRegions);
				terrainIterator.reset();
				state = TerrainIterator.IDLE;
			}

			if (state == TerrainIterator.IDLE && (shouldUpdateVisibility || viewVersion != frustum.viewVersion() || occluderVersion != TerrainOccluder.version())) {
				viewVersion = frustum.viewVersion();
				occluderVersion = TerrainOccluder.version();
				shouldUpdateVisibility = false;
				TerrainOccluder.prepareScene(camera, frustum, renderRegionStorage.regionVersion());
				terrainIterator.prepare(cameraRegion, cameraBlockPos, frustum, renderDistance);
				this.regionBuilder.executor.execute(terrainIterator, -1);
			}
		} else {
			if (shouldUpdateVisibility || viewVersion != frustum.viewVersion() || occluderVersion != TerrainOccluder.version()) {
				viewVersion = frustum.viewVersion();
				occluderVersion = TerrainOccluder.version();
				shouldUpdateVisibility = false;

				TerrainOccluder.prepareScene(camera, frustum, renderRegionStorage.regionVersion());
				terrainIterator.prepare(cameraRegion, cameraBlockPos, frustum, renderDistance);
				terrainIterator.accept(null);

				final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
				final int size = terrainIterator.visibleRegionCount;
				visibleRegionCount = size;
				System.arraycopy(terrainIterator.visibleRegions, 0, visibleRegions, 0, size);
				scheduleOrBuild(terrainIterator.updateRegions);
				terrainIterator.reset();
			}
		}


		mc.getProfiler().pop();
	}

	private void scheduleOrBuild(SimpleUnorderedArrayList<BuiltRenderRegion> updateRegions)  {
		final int limit = updateRegions.size();
		final Set<BuiltRenderRegion> regionsToRebuild = this.regionsToRebuild;

		if (limit == 0 ) {
			return;
		}

		for (int i = 0; i < limit;  ++i) {
			final BuiltRenderRegion region = updateRegions.get(i);

			if (region.needsRebuild()) {
				if (region.needsImportantRebuild() || region.isNear()) {
					regionsToRebuild.remove(region);
					region.rebuildOnMainThread();
					region.markBuilt();
				} else {
					regionsToRebuild.add(region);
				}
			}
		}
	}

	private void buildNearRegion(BuiltRenderRegion region)  {
		if(region.needsRebuild())  {
			regionsToRebuild.remove(region);
			region.rebuildOnMainThread();
			region.markBuilt();
		}
	}

	public static int playerLightmap() {
		return instance == null ? 0 : instance.playerLightmap;
	}

	private void updatePlayerLightmap(MinecraftClient mc, float f) {
		playerLightmap = mc.getEntityRenderManager().getLight(mc.player, f);
	}

	public void renderWorld(MatrixStack matrixStack, float f, long startTime, boolean bl, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		updatePlayerLightmap(mc, f);
		final ClientWorld world = wr.canvas_world();
		final BufferBuilderStorage bufferBuilders = wr.canvas_bufferBuilders();

		final EntityRenderDispatcher entityRenderDispatcher = wr.canvas_entityRenderDispatcher();

		BlockEntityRenderDispatcher.INSTANCE.configure(world, mc.getTextureManager(), mc.textRenderer, camera, mc.crosshairTarget);
		entityRenderDispatcher.configure(world, camera, mc.targetedEntity);
		final Profiler profiler = world.getProfiler();
		profiler.swap("light_updates");
		mc.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
		final Vec3d vec3d = camera.getPos();
		final double cameraX = vec3d.getX();
		final double cameraY = vec3d.getY();
		final double cameraZ = vec3d.getZ();
		final Matrix4f modelMatrix = matrixStack.peek().getModel();

		profiler.swap("culling");

		final CanvasFrustum frustum = this.frustum;
		frustum.prepare(modelMatrix, projectionMatrix, camera);

		mc.getProfiler().swap("regions");
		renderRegionStorage.updateRegionOriginsIfNeeded(mc);

		profiler.swap("clear");
		BackgroundRenderer.render(camera, f, mc.world, mc.options.viewDistance, gameRenderer.getSkyDarkness(f));
		RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		final float h = gameRenderer.getViewDistance();
		final boolean bl3 = mc.world.getSkyProperties().useThickFog(MathHelper.floor(cameraX), MathHelper.floor(cameraY)) || mc.inGameHud.getBossBarHud().shouldThickenFog();
		if (mc.options.viewDistance >= 4) {
			BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, h, bl3);
			profiler.swap("sky");
			((WorldRenderer) wr).renderSky(matrixStack, f);
		}

		profiler.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(h - 16.0F, 32.0F), bl3);
		profiler.swap("terrain_setup");

		setupTerrain(camera, frustum, wr.canvas_getAndIncrementFrameIndex(), mc.player.isSpectator());

		profiler.swap("updatechunks");
		final int maxFps = mc.options.maxFps;
		long maxFpsLimit;

		if (maxFps == Option.FRAMERATE_LIMIT.getMax()) {
			maxFpsLimit = 0L;
		} else {
			maxFpsLimit = 1000000000 / maxFps;
		}

		final long budget = Util.getMeasuringTimeNano() - startTime;

		// No idea wtg the 3/2 is for - looks like a hack
		final long updateBudget = wr.canvas_chunkUpdateSmoother().getTargetUsedTime(budget) * 3L / 2L;
		final long clampedBudget = MathHelper.clamp(updateBudget, maxFpsLimit, 33333333L);

		updateRegions(startTime + clampedBudget);

		LightmapHdTexture.instance().onRenderTick();

		profiler.swap("terrain");
		renderTerrainLayer(false, matrixStack, cameraX, cameraY, cameraZ);
		DiffuseLighting.enableForLevel(matrixStack.peek().getModel());

		profiler.swap("entities");
		profiler.push("prepare");
		int entityCount = 0;
		profiler.swap("entities");

		final boolean canDrawEntityOutlines = wr.canvas_canDrawEntityOutlines();

		if (canDrawEntityOutlines) {
			wr.canvas_entityOutlinesFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
			mc.getFramebuffer().beginWrite(false);
		}

		boolean bl4 = false;
		final VertexConsumerProvider.Immediate immediate = bufferBuilders.getEntityVertexConsumers();
		final Iterator<Entity> entities = world.getEntities().iterator();
		final ShaderEffect entityOutlineShader = wr.canvas_entityOutlineShader();
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;

		while(true) {
			Entity entity;
			int x;
			do {
				do {
					do {
						if (!entities.hasNext()) {
							immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							profiler.swap("blockentities");

							final int visibleRegionCount = this.visibleRegionCount;
							final Set<BlockEntity> noCullingBlockEntities = wr.canvas_noCullingBlockEntities();
							int regionIndex = 0;

							while(true) {
								List<BlockEntity> list;
								do {
									if (regionIndex == visibleRegionCount) {
										// exits here
										synchronized(noCullingBlockEntities) {
											final Iterator<BlockEntity> var56 = noCullingBlockEntities.iterator();

											while(true) {
												if (!var56.hasNext()) {
													break;
												}

												final BlockEntity blockEntity2 = var56.next();
												final BlockPos blockPos2 = blockEntity2.getPos();
												matrixStack.push();
												matrixStack.translate(blockPos2.getX() - cameraX, blockPos2.getY() - cameraY, blockPos2.getZ() - cameraZ);
												BlockEntityRenderDispatcher.INSTANCE.render(blockEntity2, f, matrixStack, immediate);
												matrixStack.pop();
											}
										}

										immediate.draw(RenderLayer.getSolid());
										immediate.draw(TexturedRenderLayers.getEntitySolid());
										immediate.draw(TexturedRenderLayers.getEntityCutout());
										immediate.draw(TexturedRenderLayers.getBeds());
										immediate.draw(TexturedRenderLayers.getShulkerBoxes());
										immediate.draw(TexturedRenderLayers.getSign());
										immediate.draw(TexturedRenderLayers.getChest());
										bufferBuilders.getOutlineVertexConsumers().draw();

										if (bl4) {
											entityOutlineShader.render(f);
											mc.getFramebuffer().beginWrite(false);
										}

										profiler.swap("destroyProgress");
										final ObjectIterator<Entry<SortedSet<BlockBreakingInfo>>> var53 = wr.canvas_blockBreakingProgressions().long2ObjectEntrySet().iterator();

										while(var53.hasNext()) {
											final Entry<SortedSet<BlockBreakingInfo>> entry = var53.next();
											final BlockPos blockPos3 = BlockPos.fromLong(entry.getLongKey());
											final double y = blockPos3.getX() - cameraX;
											final double z = blockPos3.getY() - cameraY;
											final double aa = blockPos3.getZ() - cameraZ;

											if (y * y + z * z + aa * aa <= 1024.0D) {
												final SortedSet<BlockBreakingInfo> sortedSet2 = entry.getValue();

												if (sortedSet2 != null && !sortedSet2.isEmpty()) {
													final int stage = sortedSet2.last().getStage();
													matrixStack.push();
													matrixStack.translate(blockPos3.getX() - cameraX, blockPos3.getY() - cameraY, blockPos3.getZ() - cameraZ);
													final MatrixStack.Entry xform = matrixStack.peek();
													final VertexConsumer vertexConsumer2 = new TransformingVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), xform.getModel(), xform.getNormal());
													mc.getBlockRenderManager().renderDamage(world.getBlockState(blockPos3), blockPos3, world, matrixStack, vertexConsumer2);
													matrixStack.pop();
												}
											}
										}

										profiler.pop();
										final HitResult hitResult = mc.crosshairTarget;

										if (bl && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
											profiler.swap("outline");
											final BlockPos blockPos4 = ((BlockHitResult)hitResult).getBlockPos();
											final BlockState blockState = world.getBlockState(blockPos4);

											if (!blockState.isAir() && world.getWorldBorder().contains(blockPos4)) {
												final VertexConsumer vertexConsumer3 = immediate.getBuffer(RenderLayer.getLines());
												wr.canvas_drawBlockOutline(matrixStack, vertexConsumer3, camera.getFocusedEntity(), cameraX, cameraY, cameraZ, blockPos4, blockState);
											}
										}

										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrixStack.peek().getModel());
										mc.debugRenderer.render(matrixStack, immediate, cameraX, cameraY, cameraZ);
										wr.canvas_renderWorldBorder(camera);
										RenderSystem.popMatrix();
										immediate.draw(TexturedRenderLayers.getEntityTranslucentCull());
										immediate.draw(TexturedRenderLayers.getBannerPatterns());
										immediate.draw(TexturedRenderLayers.getShieldPatterns());
										immediate.draw(RenderLayer.getGlint());
										immediate.draw(RenderLayer.getEntityGlint());
										immediate.draw(RenderLayer.getWaterMask());
										bufferBuilders.getEffectVertexConsumers().draw();
										immediate.draw(RenderLayer.getLines());
										immediate.draw();

										profiler.swap("translucent");
										renderTerrainLayer(true, matrixStack, cameraX, cameraY, cameraZ);

										profiler.swap("particles");
										mc.particleManager.renderParticles(matrixStack, immediate, lightmapTextureManager, camera, f);
										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrixStack.peek().getModel());

										if (Configurator.debugOcclusionBoxes) {
											renderCullBoxes(matrixStack, immediate, cameraX, cameraY, cameraZ, f);
										}

										profiler.swap("cloudsLayers");

										if (mc.options.getCloudRenderMode() != CloudRenderMode.OFF) {
											profiler.swap("clouds");
											((WorldRenderer) wr).renderClouds(matrixStack, f, cameraX, cameraY, cameraZ);
										}

										RenderSystem.depthMask(false);
										profiler.swap("weather");
										wr.canvas_renderWeather(lightmapTextureManager, f, cameraX, cameraY, cameraZ);
										RenderSystem.depthMask(true);
										//this.renderChunkDebugInfo(camera);
										RenderSystem.shadeModel(7424);
										RenderSystem.depthMask(true);
										RenderSystem.disableBlend();
										RenderSystem.popMatrix();
										BackgroundRenderer.method_23792();

										wr.canvas_setEntityCount(entityCount);

										return;
									}

									assert visibleRegions[regionIndex] != null;
									assert visibleRegions[regionIndex].getRenderData() != null;

									list = visibleRegions[regionIndex++].getRenderData().getBlockEntities();
								} while(list.isEmpty());

								final Iterator<BlockEntity> var60 = list.iterator();

								while(var60.hasNext()) {
									final BlockEntity blockEntity = var60.next();
									final BlockPos blockPos = blockEntity.getPos();
									VertexConsumerProvider vertexConsumerProvider3 = immediate;
									matrixStack.push();
									matrixStack.translate(blockPos.getX() - cameraX, blockPos.getY() - cameraY, blockPos.getZ() - cameraZ);
									final SortedSet<BlockBreakingInfo> sortedSet = wr.canvas_blockBreakingProgressions().get(blockPos.asLong());

									if (sortedSet != null && !sortedSet.isEmpty()) {
										final int stage = sortedSet.last().getStage();

										if (stage >= 0) {
											final MatrixStack.Entry xform = matrixStack.peek();
											final VertexConsumer vertexConsumer = new TransformingVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), xform.getModel(), xform.getNormal());
											vertexConsumerProvider3 = (renderLayer) -> {
												final VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
												return renderLayer.hasCrumbling() ? VertexConsumers.dual(vertexConsumer, vertexConsumer2) : vertexConsumer2;
											};
										}
									}

									BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, f, matrixStack, vertexConsumerProvider3);
									matrixStack.pop();
								}
							}
						}

						entity = entities.next();
					} while(!entityRenderDispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ) && !entity.hasPassengerDeep(mc.player));
				} while(entity == camera.getFocusedEntity() && !camera.isThirdPerson() && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping()));
			} while(entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity);

			++entityCount;
			if (entity.age == 0) {
				entity.lastRenderX = entity.getX();
				entity.lastRenderY = entity.getY();
				entity.lastRenderZ = entity.getZ();
			}

			Object vertexConsumerProvider2;
			if (canDrawEntityOutlines && entity.isGlowing()) {
				bl4 = true;
				final OutlineVertexConsumerProvider outlineVertexConsumerProvider = bufferBuilders.getOutlineVertexConsumers();
				vertexConsumerProvider2 = outlineVertexConsumerProvider;
				final int k = entity.getTeamColorValue();
				final int u = k >> 16 & 255;
				final int v = k >> 8 & 255;
				x = k & 255;
				outlineVertexConsumerProvider.setColor(u, v, x, 255);
			} else {
				vertexConsumerProvider2 = immediate;
			}

			// PERF: don't render entities if region is not visible and outlines are off
			wr.canvas_renderEntity(entity, cameraX, cameraY, cameraZ, f, matrixStack, (VertexConsumerProvider)vertexConsumerProvider2);
		}
	}

	private void renderCullBoxes(MatrixStack matrixStack, Immediate immediate, double cameraX, double cameraY, double cameraZ,  float tickDelta) {
		@SuppressWarnings("resource")
		final Entity entity = MinecraftClient.getInstance().gameRenderer.getCamera().getFocusedEntity();

		final HitResult hit = entity.rayTrace(12 * 16, tickDelta, true);

		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		final BlockPos pos = ((BlockHitResult) (hit)).getBlockPos();
		final BuiltRenderRegion region = renderRegionStorage.getRegion(pos);

		if (region == null) {
			return;
		}

		final int[] boxes = region.getRenderData().getOcclusionData();

		if (boxes == null || boxes.length < OcclusionRegion.CULL_DATA_FIRST_BOX) {
			return;
		}

		RenderSystem.pushMatrix();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableTexture();

		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuffer();

		final int cb = boxes[0];
		final int limit = boxes.length;

		final double x = (pos.getX() & ~0xF) - cameraX;
		final double y = (pos.getY() & ~0xF) - cameraY;
		final double z = (pos.getZ() & ~0xF) - cameraZ;

		RenderSystem.lineWidth(6.0F);
		bufferBuilder.begin(GL21.GL_LINES, VertexFormats.POSITION_COLOR);
		final int regionRange = region.occlusionRange;

		drawOutline(bufferBuilder, x + PackedBox.x0(cb), y + PackedBox.y0(cb), z + PackedBox.z0(cb), x + PackedBox.x1(cb), y + PackedBox.y1(cb), z + PackedBox.z1(cb), 0xFFAAAAAA);

		for (int i = OcclusionRegion.CULL_DATA_FIRST_BOX; i < limit; ++i) {
			final int b = boxes[i];
			final int range = PackedBox.range(b);

			if (regionRange > range) {
				break;
			}

			drawOutline(bufferBuilder, x + PackedBox.x0(b), y + PackedBox.y0(b), z + PackedBox.z0(b), x + PackedBox.x1(b), y + PackedBox.y1(b), z + PackedBox.z1(b), rangeColor(range));
		}

		tessellator.draw();
		RenderSystem.disableDepthTest();
		RenderSystem.lineWidth(3.0F);
		bufferBuilder.begin(GL21.GL_LINES, VertexFormats.POSITION_COLOR);

		drawOutline(bufferBuilder, x + PackedBox.x0(cb), y + PackedBox.y0(cb), z + PackedBox.z0(cb), x + PackedBox.x1(cb), y + PackedBox.y1(cb), z + PackedBox.z1(cb), 0xFFAAAAAA);

		for (int i = OcclusionRegion.CULL_DATA_FIRST_BOX; i < limit; ++i) {
			final int b = boxes[i];
			final int range = PackedBox.range(b);

			if (regionRange > range) {
				break;
			}

			drawOutline(bufferBuilder, x + PackedBox.x0(b), y + PackedBox.y0(b), z + PackedBox.z0(b), x + PackedBox.x1(b), y + PackedBox.y1(b), z + PackedBox.z1(b), rangeColor(range));
		}

		tessellator.draw();

		RenderSystem.enableDepthTest();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		RenderSystem.popMatrix();
	}

	private static int rangeColor(int range) {
		switch (range) {
		default:
		case  PackedBox.RANGE_NEAR:
			return 0x80FF8080;

		case  PackedBox.RANGE_MID:
			return 0x80FFFF80;

		case  PackedBox.RANGE_FAR:
			return 0x8080FF80;

		case  PackedBox.RANGE_EXTREME:
			return 0x808080FF;
		}
	}

	private void drawOutline(BufferBuilder bufferBuilder, double x0, double y0, double z0, double x1, double y1, double z1, int color) {
		final int a = (color >>> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).next();

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).next();

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).next();
	}

	private void renderTerrainLayer(boolean isTranslucent, MatrixStack matrixStack, double x, double y, double z) {
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		final int visibleRegionCount = this.visibleRegionCount;

		if (visibleRegionCount == 0) {
			return;
		}

		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();

		if (isTranslucent) {
			mc.getProfiler().push("translucent_sort");

			if (translucentSortPositionVersion != frustum.positionVersion()) {
				translucentSortPositionVersion = frustum.positionVersion();

				int j = 0;
				for (int regionIndex = 0; regionIndex < visibleRegionCount; regionIndex++) {
					if (j < 15 && visibleRegions[regionIndex].scheduleSort()) {
						++j;
					}
				}
			}

			mc.getProfiler().pop();
		}

		mc.getProfiler().push("render_" + (isTranslucent ? "translucent" : "solid"));

		final int startIndex = isTranslucent ? visibleRegionCount - 1 : 0 ;
		final int endIndex = isTranslucent ? -1 : visibleRegionCount;
		final int step = isTranslucent ? -1 : 1;
		final int frameIndex = ShaderManager.INSTANCE.frameIndex();

		if (Configurator.hdLightmaps) {
			LightmapHdTexture.instance().enable();
		}

		// PERF: consider rendering solid layers in program order vs. region/buffer order
		// alternatively, consider deferred shading or ubershaders - needs testing

		for (int regionIndex = startIndex; regionIndex != endIndex; regionIndex += step) {
			final BuiltRenderRegion builtRegion = visibleRegions[regionIndex];

			if (builtRegion == null) {
				continue;
			}

			final DrawableChunk drawable = isTranslucent ? builtRegion.translucentDrawable() : builtRegion.solidDrawable();

			if (drawable != null && !drawable.isEmpty()) {
				matrixStack.push();
				final BlockPos blockPos = builtRegion.getOrigin();
				matrixStack.translate(blockPos.getX() - x, blockPos.getY() - y, blockPos.getZ() - z);
				RenderSystem.pushMatrix();
				RenderSystem.loadIdentity();
				RenderSystem.multMatrix(matrixStack.peek().getModel());
				drawable.vboBuffer.bind();

				final ObjectArrayList<DrawableDelegate> delegates = drawable.delegates();
				final int limit = delegates.size();

				for(int i = 0; i < limit; i++) {
					final DrawableDelegate d = delegates.get(i);
					final DrawHandler h = d.materialState().drawHandler;
					final MaterialConditionImpl condition = h.condition;

					if(!condition.affectBlocks || condition.compute(frameIndex)) {
						h.setup();
						d.draw();
					}
				}

				RenderSystem.popMatrix();
				matrixStack.pop();
			}
		}

		// Important this happens BEFORE anything that could affect vertex state
		if (CanvasGlHelper.isVaoEnabled()) {
			CanvasGlHelper.glBindVertexArray(0);
		}

		if (Configurator.hdLightmaps) {
			LightmapHdTexture.instance().disable();
		}

		VboBuffer.unbind();

		RenderSystem.clearCurrentColor();

		DrawHandler.teardown();

		GlStateManager.disableClientState(GL11.GL_VERTEX_ARRAY);
		CanvasGlHelper.enableAttributes(0);
		BindStateManager.unbind();
		GlProgram.deactivate();

		mc.getProfiler().pop();
	}

	private void updateRegions(long endNanos) {
		final Set<BuiltRenderRegion> regionsToRebuild = this.regionsToRebuild;

		shouldUpdateVisibility |= regionBuilder.upload();

		//final long start = Util.getMeasuringTimeNano();
		//int builtCount = 0;

		if (!regionsToRebuild.isEmpty()) {
			final Iterator<BuiltRenderRegion> iterator = regionsToRebuild.iterator();

			while(iterator.hasNext()) {
				final BuiltRenderRegion builtRegion = iterator.next();

				if (builtRegion.needsImportantRebuild()) {
					builtRegion.rebuildOnMainThread();
				} else {
					builtRegion.scheduleRebuild();
				}

				builtRegion.markBuilt();
				iterator.remove();

				// this seemed excessive
				//				++builtCount;
				//
				//				final long now = Util.getMeasuringTimeNano();
				//				final long elapsed = now - start;
				//				final long avg = elapsed / builtCount;
				//				final long remaining = endNanos - now;
				//
				//				if (remaining < avg) {
				//					break;
				//				}

				if (Util.getMeasuringTimeNano() >= endNanos) {
					break;
				}
			}
		}
	}

	//	private static final Direction[] DIRECTIONS = Direction.values();

	public int completedRegionCount() {
		int result = 0;
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		final int limit = visibleRegionCount;

		for (int i = 0; i < limit; i++) {
			final BuiltRenderRegion region = visibleRegions[i];

			if (region.solidDrawable() != null || region.translucentDrawable() != null) {
				++result;
			}
		}

		return result;
	}
}
