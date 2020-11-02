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
import grondag.canvas.shader.MaterialShaderId;
import grondag.frex.api.material.RenderMaterial;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.Identifier;

// WIP2: find way to implement efficient decal pass again, esp in terrain
public final class RenderMaterialImpl extends AbstractRenderState implements RenderMaterial {
	public final int collectorIndex;
	public final RenderState renderState;
	public final int shaderFlags;

	RenderMaterialImpl(long bits) {
		super(nextIndex.getAndIncrement(), bits);
		collectorIndex = CollectorIndexMap.indexFromKey(collectorKey());
		renderState = CollectorIndexMap.renderStateForIndex(collectorIndex);
		shaderFlags = shaderFlags();

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

	public static final RenderMaterialImpl MISSING = new RenderMaterialImpl(0);

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
		sb.append("primaryTargetTransparency: ").append(primaryTargetTransparency).append("\n");
		sb.append("target: ").append(target.name()).append("\n");
		sb.append("texture: ").append(texture.index).append("  ").append(texture.id.toString()).append("\n");
		sb.append("bilinear: ").append(bilinear).append("\n");
		sb.append("transparency: ").append(transparency.name()).append("\n");
		sb.append("depthTest: ").append(depthTest.name()).append("\n");
		sb.append("cull: ").append(cull).append("\n");
		sb.append("writeMask: ").append(writeMask.name()).append("\n");
		sb.append("enableLightmap: ").append(enableLightmap).append("\n");
		sb.append("decal: ").append(decal.name()).append("\n");
		sb.append("lines: ").append(lines).append("\n");
		sb.append("fog: ").append(fog.name()).append("\n");

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
		sb.append("blendMode: ").append(blendMode.name()).append("\n");
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
}
