/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.material.state;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.shader.MaterialShaderId;

public final class MaterialImpl extends AbstractRenderState {
	public static final int MAX_MATERIAL_COUNT = RenderState.MAX_COUNT * 4;

	public final int collectorIndex;
	public final RenderState renderState;
	public final int shaderFlags;

	/** Vanilla render layer name if we derived from a vanilla render layer. */
	public final String label;

	MaterialImpl(long bits, String label) {
		super(nextIndex.getAndIncrement(), bits);
		collectorIndex = CollectorIndexMap.indexFromKey(collectorKey());
		renderState = CollectorIndexMap.renderStateForIndex(collectorIndex);
		shaderFlags = shaderFlags();
		this.label = label;

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
	static final MaterialImpl[] VALUES = new MaterialImpl[MAX_MATERIAL_COUNT];
	static final Long2ObjectOpenHashMap<MaterialImpl> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	public static MaterialImpl fromIndex(int index) {
		return VALUES[index];
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
		sb.append("renderLayerName: ").append(label).append("\n");
		sb.append("primaryTargetTransparency: ").append(primaryTargetTransparency).append("\n");
		sb.append("target: ").append(target.name).append("\n");
		sb.append("texture: ").append(texture.index).append("  ").append(texture.id.toString()).append("\n");
		sb.append("blur: ").append(blur).append("\n");
		sb.append("transparency: ").append(transparency.name).append("\n");
		sb.append("depthTest: ").append(depthTest.name).append("\n");
		sb.append("cull: ").append(cull).append("\n");
		sb.append("writeMask: ").append(writeMask.name).append("\n");
		sb.append("enableGlint: ").append(enableGlint).append("\n");
		sb.append("decal: ").append(decal.name).append("\n");
		sb.append("lines: ").append(lines).append("\n");
		sb.append("fog: ").append(fog).append("\n");

		sb.append("sorted: ").append(sorted).append("\n");
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
		sb.append("hurtOverlay: ").append(hurtOverlay).append("\n");
		sb.append("flashoverlay: ").append(flashOverlay).append("\n");

		sb.append("shaderFlags: ").append(Integer.toBinaryString(shaderFlags)).append("\n");
		sb.append("preset: ").append(preset).append("\n");
		sb.append("drawPriority: ").append(renderState.drawPriority).append("\n");
		return sb.toString();
	}

	public int index() {
		return index;
	}

	public String label() {
		return label;
	}

	@Override
	public int textureIndex() {
		return materialTexture.index();
	}

	@Override
	public int shaderIndex() {
		return shaderId.index;
	}
}
