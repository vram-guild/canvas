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

package grondag.canvas.buffer.input;

import java.nio.IntBuffer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.base.renderer.mesh.BaseQuadView;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;

public class CompoundVertexCollector extends BaseVertexCollector {
	private final VertexBucketFunction bucketFunction;
	private final int bucketCount;
	private final SimpleVertexCollector[] collectors;

	public CompoundVertexCollector(RenderState renderState, boolean isTerrain, int[] target) {
		super(renderState, isTerrain ? TerrainFormat.TERRAIN_MATERIAL.quadStrideInts : CanvasVertexFormats.STANDARD_MATERIAL_FORMAT.quadStrideInts, target);

		// WIP add support for entity pass and for shadow on terrain
		assert isTerrain;
		bucketCount = VertexBucketFunction.TERRAIN_BUCKET_COUNT;
		bucketFunction = VertexBucketFunction.TERRAIN;

		collectors = new SimpleVertexCollector[bucketCount];

		for (int i = 0; i < bucketCount; ++i) {
			collectors[i] = new SimpleVertexCollector(renderState, isTerrain, target);
		}
	}

	@Override
	public void commit(BaseQuadView quad, RenderMaterial mat) {
		collectors[bucketFunction.computeBucket(quad, mat)].commit();
		integerSize += quadStrideInts;
	}

	@Override
	public final void clear() {
		integerSize = 0;

		for (int i = 0; i < bucketCount; ++i) {
			collectors[i].clear();
		}
	}

	@Override
	public VertexBucket[] vertexBuckets() {
		final VertexBucket[] result = new VertexBucket[bucketCount];
		int index = 0;

		for (int i = 0; i < bucketCount; ++i) {
			final int vertexCount = collectors[i].vertexCount();
			result[i] = new VertexBucket(index, vertexCount);
			index += vertexCount;
		}

		return result;
	}

	@Override
	public void commit(int size) {
		throw new UnsupportedOperationException("Commit on compount collector must provide quad and material");
	}

	@Override
	public void toBuffer(IntBuffer intBuffer, int targetIndex) {
		for (int i = 0; i < bucketCount; ++i) {
			final var c = collectors[i];

			if (!c.isEmpty()) {
				collectors[i].toBuffer(intBuffer, targetIndex);
				targetIndex += collectors[i].integerSize;
			}
		}
	}

	@Override
	public void toBuffer(TransferBuffer targetBuffer, int bufferTargetIndex) {
		for (int i = 0; i < bucketCount; ++i) {
			final var c = collectors[i];

			if (!c.isEmpty()) {
				collectors[i].toBuffer(targetBuffer, bufferTargetIndex);
				bufferTargetIndex += collectors[i].integerSize;
			}
		}
	}

	@Override
	public void sortIfNeeded() {
		// NOOP
	}

	@Override
	public boolean sorted() {
		return false;
	}

	@Override
	public boolean sortTerrainQuads(Vec3 sortPos, RegionRenderSector sector) {
		throw new UnsupportedOperationException("Compound vertex collector does not support sortTerrainQuads.");
	}

	@Override
	public @Nullable int[] saveState(@Nullable int[] translucentState) {
		throw new UnsupportedOperationException("Compound vertex collector does not support saveState.");
	}

	@Override
	public void loadState(int[] state) {
		throw new UnsupportedOperationException("Compound vertex collector does not support loadState");
	}
}
