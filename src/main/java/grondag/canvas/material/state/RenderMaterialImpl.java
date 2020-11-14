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

package grondag.canvas.material.state;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Strings;
import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.shader.MaterialShaderId;
import grondag.fermion.bits.BitPacker64;
import grondag.frex.api.material.RenderMaterial;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.Identifier;

public final class RenderMaterialImpl extends AbstractRenderState implements RenderMaterial {
	// packs render order sorting weights - higher (later) weights are drawn first
	// assumes draws are for a single target and primitive type, so those are not included
	private static final BitPacker64<Void> SORT_PACKER = new BitPacker64<> (null, null);

	// these aren't order-dependent, they are included in sort to minimize state changes
	private static final BitPacker64<Void>.BooleanElement SORT_BLUR = SORT_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.IntElement SORT_DEPTH_TEST = SORT_PACKER.createIntElement(MaterialDepthTest.DEPTH_TEST_COUNT);
	private static final BitPacker64<Void>.BooleanElement SORT_CULL = SORT_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement SORT_LINES = SORT_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.IntElement SORT_FOG = SORT_PACKER.createIntElement(MaterialFog.FOG_COUNT);
	private static final BitPacker64<Void>.BooleanElement SORT_ENABLE_LIGHTMAP = SORT_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.IntElement SORT_SHADER_ID = SORT_PACKER.createIntElement(4096);

	// decal should be drawn after non-decal
	private static final BitPacker64<Void>.IntElement SORT_DECAL = SORT_PACKER.createIntElement(MaterialDecal.DECAL_COUNT);
	// primary sorted layer drawn first
	private static final BitPacker64<Void>.BooleanElement SORT_TPP = SORT_PACKER.createBooleanElement();
	// draw solid first, then various translucent layers
	private static final BitPacker64<Void>.IntElement SORT_TRANSPARENCY = SORT_PACKER.createIntElement(MaterialTransparency.TRANSPARENCY_COUNT);
	// draw things that update depth buffer first
	private static final BitPacker64<Void>.IntElement SORT_WRITE_MASK = SORT_PACKER.createIntElement(MaterialWriteMask.WRITE_MASK_COUNT);


	public final int collectorIndex;
	public final RenderState renderState;
	public final int shaderFlags;
	public final long drawPriority;
	/** vanilla render layer name if we derived from a vanilla render layer */
	public final String renderLayerName;

	RenderMaterialImpl(long bits, String renderLayerName) {
		super(nextIndex.getAndIncrement(), bits);
		collectorIndex = CollectorIndexMap.indexFromKey(collectorKey());
		renderState = CollectorIndexMap.renderStateForIndex(collectorIndex);
		shaderFlags = shaderFlags();
		drawPriority = drawPriority();
		this.renderLayerName = renderLayerName;

		if (Configurator.logMaterials) {
			CanvasMod.LOG.info("New RenderMaterial" + "\n" + toString() + "\n");
		}
	}

	private static ThreadLocal<MaterialFinderImpl> FINDER = ThreadLocal.withInitial(MaterialFinderImpl::new);

	public static MaterialFinderImpl finder() {
		final MaterialFinderImpl result = FINDER.get();
		result.clear();
		return result;
	}

	static AtomicInteger nextIndex = new AtomicInteger();
	static final ObjectArrayList<RenderMaterialImpl> LIST = new ObjectArrayList<>();
	static final Long2ObjectOpenHashMap<RenderMaterialImpl> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	public static final RenderMaterialImpl MISSING = new RenderMaterialImpl(0, "<canvas missing>");

	static {
		LIST.add(MISSING);
	}

	public static RenderMaterialImpl fromIndex(int index) {
		return LIST.get(index);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("stateIndex:   ").append(index).append("\n");
		sb.append("stateKey      ").append(Strings.padStart(Long.toHexString(bits), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(bits), 64, '0')).append("\n");
		sb.append("collectorIdx: ").append(collectorIndex).append("\n");
		sb.append("collectorKey: ").append(Strings.padStart(Long.toHexString(collectorKey()), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(collectorKey()), 64, '0')).append("\n");
		sb.append("renderIndex:  ").append(renderState.index).append("\n");
		sb.append("renderKey:    ").append(Strings.padStart(Long.toHexString(renderState.bits), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(renderState.bits), 64, '0')).append("\n");
		sb.append("renderLayerName: ").append(renderLayerName).append("\n");
		sb.append("primaryTargetTransparency: ").append(primaryTargetTransparency).append("\n");
		sb.append("target: ").append(target.name).append("\n");
		sb.append("texture: ").append(texture.index).append("  ").append(texture.id.toString()).append("\n");
		sb.append("blur: ").append(blur).append("\n");
		sb.append("transparency: ").append(transparency.name).append("\n");
		sb.append("depthTest: ").append(depthTest.name).append("\n");
		sb.append("cull: ").append(cull).append("\n");
		sb.append("writeMask: ").append(writeMask.name).append("\n");
		sb.append("enableLightmap: ").append(enableLightmap).append("\n");
		sb.append("decal: ").append(decal.name).append("\n");
		sb.append("lines: ").append(lines).append("\n");
		sb.append("fog: ").append(fog.name).append("\n");

		sb.append("sorted: ").append(sorted).append("\n");
		sb.append("primitive: ").append(primitive).append("\n");
		final MaterialShaderId sid = shaderId;
		sb.append("vertexShader: ").append(sid.vertexId.toString()).append(" (").append(sid.vertexIndex).append(")\n");
		sb.append("fragmentShader: ").append(sid.fragmentId.toString()).append(" (").append(sid.fragmentIndex).append(")\n");

		sb.append("conditionIndex: ").append(condition.index).append("\n");

		sb.append("disableColorIndex: ").append(disableColorIndex).append("\n");
		sb.append("emissive: ").append(emissive).append("\n");
		sb.append("disableDiffuse: ").append(disableDiffuse).append("\n");
		sb.append("disableAo: ").append(disableAo).append("\n");
		sb.append("cutout: ").append(cutout).append("\n");
		sb.append("unmipped: ").append(unmipped).append("\n");
		sb.append("transparentCutout: ").append(translucentCutout).append("\n");
		sb.append("hurtOverlay: ").append(hurtOverlay).append("\n");
		sb.append("flashoverlay: ").append(flashOverlay).append("\n");

		sb.append("shaderFlags: ").append(Integer.toBinaryString(shaderFlags)).append("\n");
		sb.append("blendMode: ").append(blendMode == null ? "null" : blendMode.name()).append("\n");
		sb.append("drawPriority: ").append(drawPriority).append("\n");
		return sb.toString();
	}

	@Override
	public Identifier vertexShader() {
		return vertexShaderSource;
	}

	@Override
	public Identifier fragmentShader() {
		return fragmentShaderSource;
	}

	private long drawPriority() {
		long result = SORT_BLUR.setValue(blur, 0);
		result = SORT_DEPTH_TEST.setValue(depthTest.index, result);
		result = SORT_CULL.setValue(cull, result);
		result = SORT_LINES.setValue(lines, result);
		result = SORT_FOG.setValue(fog.index, result);
		result = SORT_ENABLE_LIGHTMAP.setValue(enableLightmap, result);
		result = SORT_SHADER_ID.setValue(shader.index, result);
		result = SORT_DECAL.setValue(decal.drawPriority, result);
		// inverted because higher goes first
		result = SORT_TPP.setValue(!primaryTargetTransparency, result);
		result = SORT_TRANSPARENCY.setValue(transparency.drawPriority, result);
		result = SORT_WRITE_MASK.setValue(writeMask.drawPriority, result);

		return result;
	}

	@Override
	public Identifier texture() {
		return texture.id;
	}
}
