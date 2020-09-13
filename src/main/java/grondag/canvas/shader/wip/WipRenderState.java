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

package grondag.canvas.shader.wip;

import javax.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.mixin.AccessMultiPhaseParameters;
import grondag.canvas.mixin.AccessTexture;
import grondag.canvas.mixinterface.EntityRenderDispatcherExt;
import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.wip.encoding.WipVertexCollectorImpl;
import grondag.canvas.shader.wip.encoding.WipVertexFormat;
import grondag.canvas.shader.wip.props.WipDecal;
import grondag.canvas.shader.wip.props.WipDepthTest;
import grondag.canvas.shader.wip.props.WipFog;
import grondag.canvas.shader.wip.props.WipModelOrigin;
import grondag.canvas.shader.wip.props.WipTarget;
import grondag.canvas.shader.wip.props.WipTextureState;
import grondag.canvas.shader.wip.props.WipTransparency;
import grondag.canvas.shader.wip.props.WipWriteMask;
import grondag.fermion.bits.BitPacker64;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
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
public class WipRenderState {
	private final long bits;
	public final int index;

	/**
	 * true only for translucent
	 */
	public boolean sorted() {
		return SORTED.getValue(bits);
	}

	/**
	 * True when the material has vertex color and thus
	 * color should be included in the vertex format.
	 */
	public boolean hasColor() {
		return HAS_COLOR.getValue(bits);
	}

	/**
	 * True when material has vertex normals and thus
	 * normals should be included in the vertex format.
	 */
	public boolean hasNormal() {
		return HAS_NORMAL.getValue(bits);
	}

	//	/**
	//	 * True when material has a primary texture and thus
	//	 * includes UV coordinates in the vertex format.
	//	 * Canvas may compact or alter UV packing to allow for
	//	 * multi-map textures that share UV coordinates.
	//	 */
	//	public boolean hasTexture() {
	//		return HAS_TEXTURE.getValue(bits);
	//	}

	/**
	 * True if world lighting is passed to the renderer for this material.
	 * In vanilla lighting, this is done by a lightmap UV coordinate.
	 * Canvas may compact this, or may not pass it to the renderer at all,
	 * depending on the lighting model. True still indicates the material
	 * should be affected by world lighting.<p>
	 *
	 * UGLY: should this be hasWorldLight instead?  Semantics are messy.
	 */
	public boolean hasLightMap() {
		return HAS_LIGHTMAP.getValue(bits);
	}

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
	public int primitive() {
		return PRIMITIVE.getValue(bits);
	}

	public final WipVertexFormat format;
	public final WipModelOrigin modelOrigin;
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

	private WipRenderState(long bits) {
		this.bits = bits;
		modelOrigin = ORIGIN.getValue(bits);
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

		format = WipVertexFormat.forFlags(
			HAS_COLOR.getValue(bits),
			texture != WipTextureState.NO_TEXTURE,
			texture.isAtlas() || HAS_CONDITION.getValue(bits),
			HAS_LIGHTMAP.getValue(bits),
			HAS_NORMAL.getValue(bits));

		vertexStrideInts = format.vertexStrideInts;

		translucency = TRANSPARENCY.getValue(bits);

		index = nextIndex++;
	}

	@SuppressWarnings("resource")
	public void draw(WipVertexCollectorImpl collector) {
		if (texture == WipTextureState.NO_TEXTURE) {
			RenderSystem.disableTexture();
		} else {
			RenderSystem.enableTexture();
			final AbstractTexture tex = texture.texture();
			tex.bindTexture();
			tex.setFilter(bilinear, true);
		}

		// WIP (PERF): check for need to change GL state based on flag comparison
		// WIP (PERF): sort draws somehow to avoid unneeded state changes
		translucency.action.run();
		depthTest.action.run();
		writeMask.action.run();
		fog.action.run();
		decal.startAction.run();
		target.startAction.run();
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

		// TODO split this to start and end methods so draw can be done independently or with different uniforms/buffers - most instances don't need an end

		// PERF:  very very inefficient
		final ByteBuffer buffer = TransferBufferAllocator.claim(collector.byteSize());

		// WIP handle sorting
		//		if (sorted()) {
		//			collector.sortQuads(x, y, z);
		//		}

		final IntBuffer intBuffer = buffer.asIntBuffer();
		intBuffer.position(0);
		collector.toBuffer(intBuffer);

		format.enableDirect(MemoryUtil.memAddress(buffer));
		WipShader.DEFAULT_SOLID.activate();
		GlStateManager.drawArrays(primitive(), 0, collector.vertexCount());
		MaterialVertexFormat.disableDirect();
		GlProgram.deactivate();

		TransferBufferAllocator.release(buffer);

		RenderSystem.shadeModel(7424);
		decal.endAction.run();
		target.endAction.run();
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
	private static final BitPacker64.BooleanElement SORTED = PACKER.createBooleanElement();
	private static final BitPacker64.IntElement PRIMITIVE = PACKER.createIntElement(8);
	private static final BitPacker64.BooleanElement HAS_COLOR = PACKER.createBooleanElement();
	private static final BitPacker64.BooleanElement HAS_LIGHTMAP = PACKER.createBooleanElement();
	private static final BitPacker64.BooleanElement HAS_NORMAL = PACKER.createBooleanElement();
	private static final BitPacker64.BooleanElement HAS_CONDITION = PACKER.createBooleanElement();
	private static final BitPacker64<Void>.EnumElement<WipModelOrigin> ORIGIN = PACKER.createEnumElement(WipModelOrigin.class);

	private static final CacheLoader<RenderLayer, WipRenderState> LOADER = CacheLoader.from(l -> finder().copyFromLayer(l));
	private static final LoadingCache<RenderLayer, WipRenderState> LAYER_CACHE = CacheBuilder.newBuilder().weakKeys().maximumSize(32768).build(LOADER);

	public static WipRenderState fromLayer(RenderLayer renderLayer) {
		try {
			return LAYER_CACHE.get(renderLayer);
		} catch (final ExecutionException e) {
			assert false : "Unable to construct render state from layer";
		return null;
		}
	}

	public static WipRenderState fromIndex(int index) {
		return STATES[index];
	}

	private static ThreadLocal<Finder> BUILDER = ThreadLocal.withInitial(Finder::new);

	public static Finder finder() {
		final Finder result = BUILDER.get();
		result.bits = 0;
		return result;
	}

	public static class Finder {
		long bits;

		public Finder reset() {
			bits = 0;
			return this;
		}

		public WipRenderState copyFromLayer(RenderLayer layer) {
			// PERF: need faster exclusion method
			if (layer == ENTITY_SHADOW) {
				return MISSING;
			}

			final VertexFormat format = layer.getVertexFormat();
			for (final VertexFormatElement e : format.getElements()) {
				switch(e.getType()) {
					case COLOR:
						hasColor(true);
						break;
					case NORMAL:
						hasNormal(true);
						break;
					case UV:
						if (e.getFormat() == VertexFormatElement.Format.SHORT) {
							hasLightmap(true);
						}

						break;
					default:
						break;
				}
			}

			final AccessMultiPhaseParameters params = ((MultiPhaseExt) layer).canvas_phases();

			// Skip GUI and lines for now
			if (params.getLineWidth() != RenderPhase.FULL_LINE_WIDTH || !HAS_NORMAL.getValue(bits)) {
				return MISSING;
			}

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

		public Finder sorted(boolean sorted) {
			bits = SORTED.setValue(sorted, bits);
			return this;
		}

		public Finder hasColor(boolean hasColor) {
			bits = HAS_COLOR.setValue(hasColor, bits);
			return this;
		}

		public Finder hasLightmap(boolean hasLightmap) {
			bits = HAS_LIGHTMAP.setValue(hasLightmap, bits);
			return this;
		}

		public Finder hasNormal(boolean hasNormal) {
			bits = HAS_NORMAL.setValue(hasNormal, bits);
			return this;
		}

		public Finder hasCondition(boolean hasCondition) {
			bits = HAS_CONDITION.setValue(hasCondition, bits);
			return this;
		}

		public Finder primitive(int primitive) {
			assert primitive <= 7;
			bits = PRIMITIVE.setValue(primitive, bits);
			return this;
		}

		public Finder texture(@Nullable Identifier id) {
			final int val = id == null ? WipTextureState.NO_TEXTURE.index : WipTextureState.fromId(id).index;
			bits = TEXTURE.setValue(val, bits);
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

		// PERF: use copy-on-write instead of synch
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

	public static final WipRenderState MISSING = new WipRenderState(0);
	private static final RenderLayer ENTITY_SHADOW = ((EntityRenderDispatcherExt) MinecraftClient.getInstance().getEntityRenderDispatcher()).canvas_shadowLayer();

	static {
		assert PACKER.bitLength() <= 64;
		STATES[0] = MISSING;
	}
}
