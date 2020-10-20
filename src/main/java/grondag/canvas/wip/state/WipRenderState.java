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

package grondag.canvas.wip.state;

import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.Configurator;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.mixin.AccessMultiPhaseParameters;
import grondag.canvas.mixin.AccessTexture;
import grondag.canvas.mixinterface.EntityRenderDispatcherExt;
import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.canvas.pipeline.CanvasFrameBufferHacks;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.canvas.wip.shader.WipGlProgram;
import grondag.canvas.wip.shader.WipMaterialShaderImpl;
import grondag.canvas.wip.shader.WipMaterialShaderManager;
import grondag.canvas.wip.shader.WipShaderData;
import grondag.canvas.wip.state.property.WipDecal;
import grondag.canvas.wip.state.property.WipDepthTest;
import grondag.canvas.wip.state.property.WipFog;
import grondag.canvas.wip.state.property.WipTarget;
import grondag.canvas.wip.state.property.WipTextureState;
import grondag.canvas.wip.state.property.WipTransparency;
import grondag.canvas.wip.state.property.WipWriteMask;
import grondag.fermion.bits.BitPacker64;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

/**
 * Primitives with the same state have the same vertex encoding,
 * same uniform state and same GL draw state. Analogous to RenderLayer<p>
 *
 * Also serves as the key for vertex collection. Primitives with the same state
 * can share the same draw call and should be packed contiguously in the buffer.<p>
 *
 * Primitives must have the same sorting requirements, which for all but the translucent
 * collection keys means there is no sorting. Translucent primitives that require sorting
 * all belong to a small handful of collectors.<p>
 *
 * Vertex data with different state can share the same buffer and should be
 * packed in glState, uniformState order for best performance.
 */
@SuppressWarnings("rawtypes")
public final class WipRenderState {
	protected final long bits;

	public final int index;

	/**
	 * OpenGL primitive constant. Determines number of vertices.
	 *
	 * Currently used in vanilla are...
	 * GL_LINES
	 * GL_LINE_STRIP (currently GUI only)
	 * GL_TRIANGLE_STRIP (currently GUI only)
	 * GL_TRIANGLE_FAN (currently GUI only)
	 * GL_QUADS
	 */
	public final int primitive;

	public final int vertexStrideInts;
	public final WipTextureState texture;
	public final boolean bilinear;
	public final WipTransparency translucency;
	public final WipDepthTest depthTest;
	public final boolean cull;
	public final WipWriteMask writeMask;
	public final boolean enableLightmap;
	public final WipDecal decal;
	public final WipTarget target;
	public final boolean lines;
	public final WipFog fog;
	public final WipMaterialShaderImpl shader;

	/**
	 * True when translucent transparency and targets the terrain layer.
	 * Should not be rendered until that framebuffer is initialized in fabulous mode
	 * or should be delayed to render with other trasnslucent when not.
	 */
	public final boolean isTranslucentTerrain;

	private WipRenderState(long bits) {
		this.bits = bits;
		index = nextIndex++;
		primitive = PRIMITIVE.getValue(bits);
		texture = WipTextureState.fromIndex(TEXTURE.getValue(bits));
		bilinear = BILINEAR.getValue(bits);
		depthTest = DEPTH_TEST.getValue(bits);
		cull = CULL.getValue(bits);
		writeMask = WRITE_MASK.getValue(bits);
		enableLightmap = ENABLE_LIGHTMAP.getValue(bits);
		decal = DECAL.getValue(bits);
		target = TARGET.getValue(bits);
		lines = LINES.getValue(bits);
		fog = FOG.getValue(bits);
		vertexStrideInts = MaterialVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL.vertexStrideInts;
		translucency = TRANSPARENCY.getValue(bits);
		shader = WipMaterialShaderManager.INSTANCE.find(VERTEX_SHADER.getValue(bits), FRAGMENT_SHADER.getValue(bits), translucency == WipTransparency.TRANSLUCENT ? WipProgramType.MATERIAL_VERTEX_LOGIC : WipProgramType.MATERIAL_UNIFORM_LOGIC);
		isTranslucentTerrain = (target == WipTarget.MAIN || target == WipTarget.TRANSLUCENT) && translucency == WipTransparency.TRANSLUCENT;
	}

	@SuppressWarnings("resource")
	public void enable() {
		target.enable();

		if (texture == WipTextureState.NO_TEXTURE) {
			RenderSystem.disableTexture();
		} else {
			RenderSystem.enableTexture();
			final AbstractTexture tex = texture.texture();
			tex.bindTexture();
			tex.setFilter(bilinear, true);

			if (texture.isAtlas()) {
				texture.atlasInfo().enable();
			}
		}

		translucency.action.run();
		depthTest.action.run();
		writeMask.action.run();
		fog.action.run();
		decal.enable();

		// NB: must be after frame-buffer target switch
		if (Configurator.enableBloom) {
			CanvasFrameBufferHacks.startEmissiveCapture();
		} else {
			CanvasFrameBufferHacks.endEmissiveCapture();
		}

		RenderSystem.shadeModel(GL11.GL_SMOOTH);

		if (cull) {
			RenderSystem.enableCull();
		} else {
			RenderSystem.disableCull();
		}

		if (enableLightmap) {
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
		} else {
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
		}

		if (lines) {
			RenderSystem.lineWidth(Math.max(2.5F, MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 1920.0F * 2.5F));
		} else {
			RenderSystem.lineWidth(1.0F);
		}

		shader.activate(texture.atlasInfo());
	}

	public static void disable() {
		// NB: must be before frame-buffer target switch
		if (Configurator.enableBloom) CanvasFrameBufferHacks.endEmissiveCapture();

		MaterialVertexFormat.disableDirect();
		WipGlProgram.deactivate();
		RenderSystem.shadeModel(GL11.GL_FLAT);
		SpriteInfoTexture.disable();
		WipDecal.disable();
		WipTarget.disable();
	}

	public static final int MAX_COUNT = 4096;
	private static int nextIndex = 0;
	private static final WipRenderState[] STATES = new WipRenderState[MAX_COUNT];
	private static final Long2ObjectOpenHashMap<WipRenderState> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	private static final BitPacker64<Void> PACKER = new BitPacker64<> (null, null);

	// GL State comes first for sorting
	private static final BitPacker64.IntElement TEXTURE = PACKER.createIntElement(WipTextureState.MAX_TEXTURE_STATES);
	private static final BitPacker64.BooleanElement BILINEAR = PACKER.createBooleanElement();

	private static final BitPacker64<Void>.EnumElement<WipTransparency> TRANSPARENCY = PACKER.createEnumElement(WipTransparency.class);
	private static final BitPacker64<Void>.EnumElement<WipDepthTest> DEPTH_TEST = PACKER.createEnumElement(WipDepthTest.class);
	private static final BitPacker64.BooleanElement CULL = PACKER.createBooleanElement();
	private static final BitPacker64<Void>.EnumElement<WipWriteMask> WRITE_MASK = PACKER.createEnumElement(WipWriteMask.class);
	private static final BitPacker64.BooleanElement ENABLE_LIGHTMAP = PACKER.createBooleanElement();
	private static final BitPacker64<Void>.EnumElement<WipDecal> DECAL = PACKER.createEnumElement(WipDecal.class);
	private static final BitPacker64<Void>.EnumElement<WipTarget> TARGET = PACKER.createEnumElement(WipTarget.class);
	private static final BitPacker64.BooleanElement LINES = PACKER.createBooleanElement();
	private static final BitPacker64<Void>.EnumElement<WipFog> FOG = PACKER.createEnumElement(WipFog.class);

	// These don't affect GL state but do affect encoding - must be buffered separately
	private static final BitPacker64.IntElement PRIMITIVE = PACKER.createIntElement(8);

	private static final BitPacker64.IntElement VERTEX_SHADER = PACKER.createIntElement(4096);
	private static final BitPacker64.IntElement FRAGMENT_SHADER = PACKER.createIntElement(4096);

	private static final ReferenceOpenHashSet<RenderLayer> EXCLUSIONS = new ReferenceOpenHashSet<>(64, Hash.VERY_FAST_LOAD_FACTOR);
	public static final WipRenderState MISSING = new WipRenderState(0);

	static {
		assert PACKER.bitLength() <= 64;
		STATES[0] = MISSING;

		// entity shadows aren't worth
		EXCLUSIONS.add(((EntityRenderDispatcherExt) MinecraftClient.getInstance().getEntityRenderDispatcher()).canvas_shadowLayer());

		// FEAT: handle more of these with shaders
		EXCLUSIONS.add(RenderLayer.getArmorGlint());
		EXCLUSIONS.add(RenderLayer.getArmorEntityGlint());
		EXCLUSIONS.add(RenderLayer.getGlint());
		EXCLUSIONS.add(RenderLayer.getDirectGlint());
		EXCLUSIONS.add(RenderLayer.method_30676());
		EXCLUSIONS.add(RenderLayer.getEntityGlint());
		EXCLUSIONS.add(RenderLayer.getDirectEntityGlint());
		EXCLUSIONS.add(RenderLayer.getLines());
		EXCLUSIONS.add(RenderLayer.getLightning());

		ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((renderLayer) -> {
			EXCLUSIONS.add(renderLayer);
		});
	}

	public static boolean isExcluded(RenderLayer layer) {
		return EXCLUSIONS.contains(layer);
	}

	public static WipRenderState fromIndex(int index) {
		return STATES[index];
	}

	private static ThreadLocal<Finder> FINDER = ThreadLocal.withInitial(Finder::new);

	public static Finder finder() {
		final Finder result = FINDER.get();
		result.reset();
		return result;
	}

	public static class Finder {
		long bits;

		public Finder reset() {
			bits = 0;
			vertexShader(WipShaderData.DEFAULT_VERTEX_SOURCE);
			fragmentShader(WipShaderData.DEFAULT_FRAGMENT_SOURCE);
			return this;
		}

		public WipRenderState copyFromLayer(RenderLayer layer) {
			if (isExcluded(layer)) {
				return MISSING;
			}

			final AccessMultiPhaseParameters params = ((MultiPhaseExt) layer).canvas_phases();
			final AccessTexture tex = (AccessTexture) params.getTexture();

			primitive(GL11.GL_QUADS);
			texture(tex.getId().orElse(null));
			transparency(WipTransparency.fromPhase(params.getTransparency()));
			depthTest(WipDepthTest.fromPhase(params.getDepthTest()));
			cull(params.getCull() == RenderPhase.ENABLE_CULLING);
			writeMask(WipWriteMask.fromPhase(params.getWriteMaskState()));
			enableLightmap(params.getLightmap() == RenderPhase.ENABLE_LIGHTMAP);
			decal(WipDecal.fromPhase(params.getLayering()));
			target(WipTarget.fromPhase(params.getTarget()));
			lines(params.getLineWidth() != RenderPhase.FULL_LINE_WIDTH);
			fog(WipFog.fromPhase(params.getFog()));

			return find();
		}

		public Finder primitive(int primitive) {
			assert primitive <= 7;
			bits = PRIMITIVE.setValue(primitive, bits);
			return this;
		}

		//		private static final Identifier EGREGIOUS_ENDERMAN_HACK = new Identifier("textures/entity/enderman/enderman.png");

		public Finder texture(@Nullable Identifier id) {
			final int val = id == null ? WipTextureState.NO_TEXTURE.index : WipTextureState.fromId(id).index;
			bits = TEXTURE.setValue(val, bits);

			// WIP2: put in proper material map hooks
			//			if (id != null && id.equals(EGREGIOUS_ENDERMAN_HACK)) {
			//				fragmentShader(new Identifier("canvas:shaders/wip/material/enderman.frag"));
			//			}

			return this;
		}

		public Finder bilinear(boolean bilinear) {
			bits = BILINEAR.setValue(bilinear, bits);
			return this;
		}

		public Finder transparency(WipTransparency transparency) {
			bits = TRANSPARENCY.setValue(transparency, bits);
			return this;
		}

		public Finder depthTest(WipDepthTest depthTest) {
			bits = DEPTH_TEST.setValue(depthTest, bits);
			return this;
		}

		public Finder cull(boolean cull) {
			bits = CULL.setValue(cull, bits);
			return this;
		}

		public Finder writeMask(WipWriteMask writeMask) {
			bits = WRITE_MASK.setValue(writeMask, bits);
			return this;
		}

		public Finder enableLightmap(boolean enableLightmap) {
			bits = ENABLE_LIGHTMAP.setValue(enableLightmap, bits);
			return this;
		}

		public Finder decal(WipDecal decal) {
			bits = DECAL.setValue(decal, bits);
			return this;
		}

		public Finder target(WipTarget target) {
			bits = TARGET.setValue(target, bits);
			return this;
		}

		public Finder lines(boolean lines) {
			bits = LINES.setValue(lines, bits);
			return this;
		}

		public Finder fog(WipFog fog) {
			bits = FOG.setValue(fog, bits);
			return this;
		}

		public Finder vertexShader(Identifier vertexSource) {
			bits = VERTEX_SHADER.setValue(WipMaterialShaderManager.vertexIndex.toHandle(vertexSource), bits);
			return this;
		}

		public Finder fragmentShader(Identifier fragmentSource) {
			bits = FRAGMENT_SHADER.setValue(WipMaterialShaderManager.fragmentIndex.toHandle(fragmentSource), bits);
			return this;
		}

		public synchronized WipRenderState find() {
			WipRenderState result = MAP.get(bits);

			if (result == null) {
				result = new WipRenderState(bits);
				MAP.put(bits, result);
				STATES[result.index] = result;
			}

			return result;
		}
	}
}
