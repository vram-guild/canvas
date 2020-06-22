package grondag.canvas.buffer.packing;

import java.util.Arrays;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.chunk.UploadableChunk;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialState;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.ShaderPass;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT
 */
public class VertexCollectorList {
	private VertexCollectorImpl[] collectors = new VertexCollectorImpl[4096];

	private int solidCount = 0;

	private final ObjectArrayList<VertexCollectorImpl> solidCollectors = new ObjectArrayList<>();

	private MaterialContext context = null;

	public VertexCollectorList() {
		collectors[MaterialState.TRANSLUCENT_INDEX] = new VertexCollectorImpl(this);
	}

	/**
	 * Used for assertions and to set material state for translucent collector .
	 */
	public void setContext(MaterialContext context) {
		this.context = context;
		collectors[MaterialState.TRANSLUCENT_INDEX].prepare(MaterialState.getDefault(context, ShaderPass.TRANSLUCENT));
	}

	/**
	 * Releases any held vertex collectors and resets state
	 */
	public void clear() {
		collectors[MaterialState.TRANSLUCENT_INDEX].clear();

		for (int i = 0; i < solidCount; i++) {
			solidCollectors.get(i).clear();
		}

		solidCount = 0;

		Arrays.fill(collectors, 1, collectors.length, null);
	}

	public final VertexCollectorImpl getIfExists(MaterialState materialState) {
		assert materialState.context == context;
		return collectors[materialState.collectorIndex];
	}

	public final VertexCollectorImpl get(MaterialState materialState) {
		assert materialState.context == context;
		final int index = materialState.collectorIndex;
		VertexCollectorImpl[] collectors = this.collectors;

		VertexCollectorImpl result;

		if (index < collectors.length) {
			result = collectors[index];
		} else {
			result = null;
			final VertexCollectorImpl[] newCollectors = new VertexCollectorImpl[MathHelper.smallestEncompassingPowerOfTwo(index)];
			System.arraycopy(collectors, 0, newCollectors, 0, collectors.length);
			collectors = newCollectors;
			this.collectors = collectors;
		}

		if(result == null) {
			assert materialState.collectorIndex != MaterialState.TRANSLUCENT_INDEX;
			result = emptySolidCollector().prepare(materialState);
			collectors[index] = result;
		}

		return result;
	}

	private VertexCollectorImpl emptySolidCollector() {
		VertexCollectorImpl result;

		if(solidCount == solidCollectors.size()) {
			result = new VertexCollectorImpl(this);
			solidCollectors.add(result);
		} else {
			result = solidCollectors.get(solidCount);
		}

		++solidCount;
		return result;
	}

	public boolean contains(MaterialState materialState) {
		assert materialState.context == context;
		final int index = materialState.collectorIndex;
		return index < collectors.length && collectors[index] != null;
	}

	private int totalBytes(boolean translucent) {
		if (translucent) {
			return collectors[MaterialState.TRANSLUCENT_INDEX].integerSize() * 4;
		} else {
			final int solidCount = this.solidCount;
			final ObjectArrayList<VertexCollectorImpl> solidCollectors = this.solidCollectors;

			int intSize = 0;

			for(int i = 0; i < solidCount; i++) {
				intSize += solidCollectors.get(i).integerSize();
			}

			return intSize * 4;
		}
	}

	public VertexCollectorImpl get(MaterialContext terrain, DrawableMaterial mat) {
		return get(MaterialState.get(terrain, mat));
	}

	public UploadableChunk toUploadableChunk(MaterialContext context, boolean isTranslucent) {
		final int bytes = totalBytes(isTranslucent);
		return bytes == 0 ? null : new UploadableChunk(this, MaterialVertexFormats.get(context, isTranslucent), isTranslucent, bytes);
	}

	public VertexCollectorImpl getTranslucent() {
		return collectors[MaterialState.TRANSLUCENT_INDEX];
	}

	public int solidCount() {
		return solidCount;
	}

	public VertexCollectorImpl getSolid(int index) {
		return solidCollectors.get(index);
	}
}
