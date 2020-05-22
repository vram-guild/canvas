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
package grondag.canvas.light;

import static grondag.canvas.apiimpl.util.GeometryHelper.AXIS_ALIGNED_FLAG;
import static grondag.canvas.apiimpl.util.GeometryHelper.CUBIC_FLAG;
import static grondag.canvas.apiimpl.util.GeometryHelper.LIGHT_FACE_FLAG;
import static grondag.canvas.chunk.RenderRegionAddressHelper.cacheIndexToXyz5;
import static grondag.canvas.chunk.RenderRegionAddressHelper.fastRelativeCacheIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.offsetMainChunkBlockIndex;
import static grondag.canvas.light.AoFaceData.OPAQUE;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.mesh.QuadViewImpl;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.light.AoFace.Vertex2Float;
import grondag.canvas.light.AoFace.WeightFunction;

/**
 * Adaptation of inner, non-static class in BlockModelRenderer that serves same
 * purpose.
 */
@Environment(EnvType.CLIENT)
public abstract class AoCalculator {
	public static final float DIVIDE_BY_255 = 1f / 255f;

	//PERF: could be better - or wait for a diff Ao model
	static final int BLEND_CACHE_DIVISION = 16;
	static final int BLEND_CACHE_DEPTH = BLEND_CACHE_DIVISION - 1;
	static final int BLEND_CACHE_ARRAY_SIZE = BLEND_CACHE_DEPTH * 6;
	static final int BLEND_INDEX_NO_DEPTH = -1;
	static final int BLEND_INDEX_FULL_DEPTH = BLEND_CACHE_DIVISION - 1;

	static int blendIndex(int face, float depth) {
		final int depthIndex = MathHelper.clamp((((int)(depth * BLEND_CACHE_DIVISION * 2 + 1)) >> 1), 1, 15) - 1;
		return face * BLEND_CACHE_DEPTH + depthIndex;
	}

	private long blendCacheCompletionLowFlags;
	private long blendCacheCompletionHighFlags;

	private int regionRelativeCacheIndex;

	private final AoFaceCalc[] blendCache = new AoFaceCalc[BLEND_CACHE_ARRAY_SIZE];

	// PERF: need to cache these vs only the calc results due to mixed use
	private final AoFaceData localData = new AoFaceData();

	/**
	 * caches results of {@link #gatherFace(Direction, boolean)} for the current
	 * block
	 */
	private final AoFaceData[] faceData = new AoFaceData[12];

	/**
	 * indicates which elements of {@link #faceData} have been computed for the
	 * current block
	 */
	private int completionFlags = 0;

	/** holds per-corner weights - used locally to avoid new allocation. */
	private final float[] w = new float[4];

	public AoCalculator() {
		for (int i = 0; i < 12; i++) {
			faceData[i] = new AoFaceData();
		}

		for (int i = 0; i < BLEND_CACHE_ARRAY_SIZE; i++) {
			blendCache[i] = new AoFaceCalc();
		}
	}

	protected abstract float ao(int cacheIndex);

	protected abstract int brightness(int cacheIndex);

	protected abstract boolean isOpaque(int cacheIndex);

	private boolean checkBlendDirty(int blendIndex) {
		if((blendIndex & 63) == 0) {
			final long mask = 1L << blendIndex;

			if ((blendCacheCompletionLowFlags & mask) == 0) {
				blendCacheCompletionLowFlags |= mask;
				return true;
			} else {
				return false;
			}
		} else {
			final long mask = 1L << (blendIndex - 64);

			if ((blendCacheCompletionHighFlags & mask) == 0) {
				blendCacheCompletionHighFlags |= mask;
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * call at start of each new block
	 * @param index region-relative index - must be an interior index - for block context, will always be 0
	 */
	public void prepare(int index) {
		regionRelativeCacheIndex = index;
		completionFlags = 0;
		blendCacheCompletionLowFlags = 0;
		blendCacheCompletionHighFlags = 0;
	}

	public void computeFlat(MutableQuadViewImpl quad, int flatBrightness) {
		if(Configurator.hdLightmaps) {
			flatFaceSmooth(quad, flatBrightness);
		} else {
			assert false : "Called block lighter for flat lighting outside HD lighting model";
		}
	}

	public void compute(MutableQuadViewImpl quad) {
		if(quad.hasVertexNormals()) {
			// these can only be lit this way
			irregularFace(quad);
			return;
		}

		final int flags = quad.geometryFlags();

		if(Configurator.hdLightmaps) {
			if((flags & AXIS_ALIGNED_FLAG) == AXIS_ALIGNED_FLAG) {
				if((flags & LIGHT_FACE_FLAG) == LIGHT_FACE_FLAG) {
					vanillaPartialFaceSmooth(quad, true);
				} else {
					blendedPartialFaceSmooth(quad);
				}
			} else {
				// currently can't handle these
				irregularFace(quad);
				quad.aoShade = null;
				quad.blockLight = null;
				quad.skyLight = null;
			}
			return;
		}

		quad.aoShade = null;
		quad.blockLight = null;
		quad.skyLight = null;

		switch (flags) {
		case AXIS_ALIGNED_FLAG | CUBIC_FLAG | LIGHT_FACE_FLAG:
		case AXIS_ALIGNED_FLAG | LIGHT_FACE_FLAG:
			blockFace(quad, true);
			break;

		case AXIS_ALIGNED_FLAG | CUBIC_FLAG:
		case AXIS_ALIGNED_FLAG:
			blendedFace(quad);
			break;

		default:
			irregularFace(quad);
			break;
		}
	}

	private void blockFace(MutableQuadViewImpl quad, boolean isOnLightFace) {
		final int lightFace = quad.lightFaceId();
		final AoFaceCalc faceData = gatherFace(lightFace, isOnLightFace).calc;
		final AoFace face = AoFace.get(lightFace);
		final WeightFunction wFunc = face.weightFunc;
		final float[] w = this.w;
		final float[] ao = quad.ao;

		for (int i = 0; i < 4; i++) {
			wFunc.apply(quad, i, w);
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), faceData.weightedCombinedLight(w)));
			ao[i] = faceData.weigtedAo(w) * DIVIDE_BY_255;
		}
	}

	private void vanillaPartialFaceSmooth(MutableQuadViewImpl quad, boolean isOnLightFace) {
		final int lightFace = quad.lightFaceId();
		final AoFaceData faceData = gatherFace(lightFace, isOnLightFace);
		final AoFace face = AoFace.get(lightFace);
		final Vertex2Float uFunc = face.uFunc;
		final Vertex2Float vFunc = face.vFunc;

		for (int i = 0; i < 4; i++) {
			quad.u[i] = uFunc.apply(quad, i);
			quad.v[i] = vFunc.apply(quad, i);
		}

		quad.aoShade = LightmapHd.findAo(faceData);
		quad.blockLight = LightmapHd.findBlock(faceData);
		quad.skyLight = LightmapHd.findSky(faceData);
	}

	private void flatFaceSmooth(MutableQuadViewImpl quad, int flatBrightness) {
		final int lightFace = quad.lightFaceId();
		final AoFaceData faceData = localData;
		faceData.setFlat(flatBrightness);
		final AoFace face = AoFace.get(lightFace);
		final Vertex2Float uFunc = face.uFunc;
		final Vertex2Float vFunc = face.vFunc;

		for (int i = 0; i < 4; i++) {
			quad.u[i] = uFunc.apply(quad, i);
			quad.v[i] = vFunc.apply(quad, i);
		}

		quad.aoShade = LightmapHd.findAo(faceData);
		quad.blockLight = LightmapHd.findBlock(faceData);
		quad.skyLight = LightmapHd.findSky(faceData);
	}

	/**
	 * Returns linearly interpolated blend of outer and inner face based on depth of
	 * vertex in face
	 */
	private AoFaceCalc blendedInsetData(QuadViewImpl quad, int vertexIndex, int lightFace) {
		final float w1 = AoFace.get(lightFace).depthFunc.apply(quad, vertexIndex);

		if (w1 <= 0.03125f) {
			return gatherFace(lightFace, true).calc;
		} else if (w1 >= 0.96875f) {
			return gatherFace(lightFace, false).calc;
		} else {
			final int blendIndex = blendIndex(lightFace, w1);
			final AoFaceCalc result = blendCache[blendIndex];

			if (checkBlendDirty(blendIndex)) {
				final float w0 = 1 - w1;
				result.weightedMean(
						gatherFace(lightFace, true).calc, w0,
						gatherFace(lightFace, false).calc, w1);
			}

			return result;
		}
	}

	private void blendedFace(MutableQuadViewImpl quad) {
		final int lightFace = quad.lightFaceId();
		final AoFaceCalc faceData = blendedInsetData(quad, 0, lightFace);
		final AoFace face = AoFace.get(lightFace);
		final WeightFunction wFunc = face.weightFunc;
		final float[] w = this.w;
		final float[] ao = quad.ao;

		for (int i = 0; i < 4; i++) {
			wFunc.apply(quad, i, w);
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), faceData.weightedCombinedLight(w)));
			ao[i] = faceData.weigtedAo(w) * DIVIDE_BY_255;
		}
	}

	private void blendedPartialFaceSmooth(MutableQuadViewImpl quad) {
		final int lightFace = quad.lightFaceId();
		final float w1 = AoFace.get(lightFace).depthFunc.apply(quad, 0);
		final float w0 = 1 - w1;
		final AoFaceData faceData = localData;

		// PERF: cache recent results somehow
		AoFaceData.blendTo(gatherFace(lightFace, true), w0, gatherFace(lightFace, false), w1, faceData);

		final AoFace face = AoFace.get(lightFace);
		final Vertex2Float uFunc = face.uFunc;
		final Vertex2Float vFunc = face.vFunc;

		for (int i = 0; i < 4; i++) {
			quad.u[i] = uFunc.apply(quad, i);
			quad.v[i] = vFunc.apply(quad, i);
		}

		quad.aoShade = LightmapHd.findAo(faceData);
		quad.skyLight = LightmapHd.findSky(faceData);
		quad.blockLight = LightmapHd.findBlock(faceData);
	}

	/**
	 * used exclusively in irregular face to avoid new heap allocations each call.
	 */
	private final Vector3f vertexNormal = new Vector3f();

	private static final int UP = Direction.UP.ordinal();
	private static final int DOWN = Direction.DOWN.ordinal();
	private static final int EAST = Direction.EAST.ordinal();
	private static final int WEST = Direction.WEST.ordinal();
	private static final int NORTH = Direction.NORTH.ordinal();
	private static final int SOUTH = Direction.SOUTH.ordinal();

	private void irregularFace(MutableQuadViewImpl quad) {
		final Vector3f faceNorm = quad.faceNormal();
		Vector3f normal;
		final float[] w = this.w;
		final float aoResult[] = quad.ao;

		//TODO: currently no way to handle 3d interpolation shader-side
		quad.blockLight = null;
		quad.skyLight = null;
		quad.aoShade = null;

		for (int i = 0; i < 4; i++) {
			normal = quad.hasNormal(i) ? quad.copyNormal(i, vertexNormal) : faceNorm;
			float ao = 0, sky = 0, block = 0;
			int maxSky = 0, maxBlock = 0;
			float maxAo = 0;

			final float x = normal.getX();
			if (!MathHelper.approximatelyEquals(0f, x)) {
				final int face = x > 0 ? EAST : WEST;
				// PERF: really need to cache these
				final AoFaceCalc fd = blendedInsetData(quad, i, face);
				AoFace.get(face).weightFunc.apply(quad, i, w);
				final float n = x * x;
				final float a = fd.weigtedAo(w);
				final int s = fd.weigtedSkyLight(w);
				final int b = fd.weigtedBlockLight(w);
				ao += n * a;
				sky += n * s;
				block += n * b;
				maxAo = a;
				maxSky = s;
				maxBlock = b;
			}

			final float y = normal.getY();
			if (!MathHelper.approximatelyEquals(0f, y)) {
				final int face = y > 0 ? UP : DOWN;
				final AoFaceCalc fd = blendedInsetData(quad, i, face);
				AoFace.get(face).weightFunc.apply(quad, i, w);
				final float n = y * y;
				final float a = fd.weigtedAo(w);
				final int s = fd.weigtedSkyLight(w);
				final int b = fd.weigtedBlockLight(w);
				ao += n * a;
				sky += n * s;
				block += n * b;
				maxAo = Math.max(a, maxAo);
				maxSky = Math.max(s, maxSky);
				maxBlock = Math.max(b, maxBlock);
			}

			final float z = normal.getZ();
			if (!MathHelper.approximatelyEquals(0f, z)) {
				final int face = z > 0 ? SOUTH : NORTH;
				final AoFaceCalc fd = blendedInsetData(quad, i, face);
				AoFace.get(face).weightFunc.apply(quad, i, w);
				final float n = z * z;
				final float a = fd.weigtedAo(w);
				final int s = fd.weigtedSkyLight(w);
				final int b = fd.weigtedBlockLight(w);
				ao += n * a;
				sky += n * s;
				block += n * b;
				maxAo = Math.max(a, maxAo);
				maxSky = Math.max(s, maxSky);
				maxBlock = Math.max(b, maxBlock);
			}

			aoResult[i] = (ao + maxAo) * (0.5f * DIVIDE_BY_255);
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), (((int) ((sky + maxSky) * 0.5f) & 0xFF) << 16)
					| ((int)((block + maxBlock) * 0.5f) & 0xFF)));
		}
	}

	/**
	 * Computes smoothed brightness and Ao shading for four corners of a block face.
	 * Outer block face is what you normally see and what you get get when second
	 * parameter is true. Inner is light *within* the block and usually darker. It
	 * is blended with the outer face for inset surfaces, but is also used directly
	 * in vanilla logic for some blocks that aren't full opaque cubes. Except for
	 * parameterization, the logic itself is practically identical to vanilla.
	 */
	private AoFaceData gatherFace(final int lightFace, boolean isOnBlockFace) {
		final int faceDataIndex = isOnBlockFace ? lightFace : lightFace + 6;
		final int mask = 1 << faceDataIndex;
		final AoFaceData fd = faceData[faceDataIndex];

		if ((completionFlags & mask) == 0) {
			completionFlags |= mask;
			updateFace(fd, lightFace, isOnBlockFace);
		}

		return fd;
	}

	private void updateFace(AoFaceData fd, final int lightFace, boolean isOnBlockFace) {
		int index = regionRelativeCacheIndex;

		// Overall this is different from vanilla, which seems to be buggy
		// basically, use neighbor pos unless it is full opaque - in that case cheat and use
		// this block's position.
		// A key difference from vanilla is that this position is then used as the center for
		// all following offsets, which avoids anisotropy in smooth lighting.
		if (isOnBlockFace) {
			final int offsetIndex = offsetMainChunkBlockIndex(index, ModelHelper.faceFromIndex(lightFace));

			if(!isOpaque(offsetIndex)) {
				index = offsetIndex;
			}
		}

		final int packedXyz5 = cacheIndexToXyz5(index);
		final int x = (packedXyz5 & 31) - 1;
		final int y = ((packedXyz5 >> 5) & 31) - 1;
		final int z = (packedXyz5 >> 10) - 1;

		fd.center = brightness(index);
		final int aoCenter = Math.round(ao(index) * 255);

		final AoFace aoFace = AoFace.get(lightFace);

		// vanilla was further offsetting these in the direction of the light face
		// but it was actually mis-sampling and causing visible artifacts in certain situation
		Vec3i offset = aoFace.bottomVec;
		int cacheIndex = fastRelativeCacheIndex(x + offset.getX() , y + offset.getY(), z + offset.getZ());
		final boolean bottomClear = !isOpaque(cacheIndex);
		fd.bottom = bottomClear ? brightness(cacheIndex) : OPAQUE;
		final int aoBottom = Math.round(ao(cacheIndex) * 255);

		offset = aoFace.topVec;
		cacheIndex = fastRelativeCacheIndex(x + offset.getX() , y + offset.getY(), z + offset.getZ());
		final boolean topClear = !isOpaque(cacheIndex);
		fd.top = topClear ? brightness(cacheIndex) : OPAQUE;
		final int aoTop = Math.round(ao(cacheIndex) * 255);

		offset = aoFace.leftVec;
		cacheIndex = fastRelativeCacheIndex(x + offset.getX() , y + offset.getY(), z + offset.getZ());
		final boolean leftClear = !isOpaque(cacheIndex);
		fd.left = leftClear ? brightness(cacheIndex) : OPAQUE;
		final int aoLeft = Math.round(ao(cacheIndex) * 255);

		offset = aoFace.rightVec;
		cacheIndex = fastRelativeCacheIndex(x + offset.getX() , y + offset.getY(), z + offset.getZ());
		final boolean rightClear = !isOpaque(cacheIndex);
		fd.right = rightClear ? brightness(cacheIndex) : OPAQUE;
		final int aoRight = Math.round(ao(cacheIndex) * 255);

		if (!(leftClear || bottomClear)) {
			// both not clear
			fd.aoBottomLeft = (Math.min(aoLeft, aoBottom) + aoBottom + aoLeft + 1 + aoCenter) >> 2;
		fd.bottomLeft = OPAQUE;
		} else { // at least one clear
			offset = aoFace.bottomLeftVec;
			cacheIndex = fastRelativeCacheIndex(x + offset.getX() , y + offset.getY(), z + offset.getZ());
			final boolean cornerClear = !isOpaque(cacheIndex);
			fd.bottomLeft = cornerClear ? brightness(cacheIndex) : OPAQUE;
			fd.aoBottomLeft = (Math.round(ao(cacheIndex) * 255) + aoBottom + aoCenter + aoLeft + 1) >> 2;  // bitwise divide by four, rounding up
		}

		if (!(rightClear || bottomClear)) {
			// both not clear
			fd.aoBottomRight = (Math.min(aoRight, aoBottom) + aoBottom + aoRight + 1 + aoCenter) >> 2;
			fd.bottomRight = OPAQUE;
		} else { // at least one clear
			offset = aoFace.bottomRightVec;
			cacheIndex = fastRelativeCacheIndex(x + offset.getX() , y + offset.getY(), z + offset.getZ());
			final boolean cornerClear = !isOpaque(cacheIndex);
			fd.bottomRight = cornerClear ? brightness(cacheIndex) : OPAQUE;
			fd.aoBottomRight = (Math.round(ao(cacheIndex) * 255) + aoBottom + aoCenter + aoRight + 1) >> 2;
		}

		if (!(leftClear || topClear)) {
			// both not clear
			fd.aoTopLeft = (Math.min(aoLeft, aoTop) + aoTop + aoLeft + 1 + aoCenter) >> 2;
		fd.topLeft = OPAQUE;
		} else { // at least one clear
			offset = aoFace.topLeftVec;
			cacheIndex = fastRelativeCacheIndex(x + offset.getX() , y + offset.getY(), z + offset.getZ());
			final boolean cornerClear = !isOpaque(cacheIndex);
			fd.topLeft = cornerClear ? brightness(cacheIndex) : OPAQUE;
			fd.aoTopLeft = (Math.round(ao(cacheIndex) * 255) + aoTop + aoCenter + aoLeft + 1) >> 2;
		}

		if (!(rightClear || topClear)) {
			// both not clear
			fd.aoTopRight = (Math.min(aoRight, aoTop) + aoTop+ aoRight + 1 + aoCenter) >> 2;
			fd.topRight = OPAQUE;
		} else { // at least one clear
			offset = aoFace.topRightVec;
			cacheIndex = fastRelativeCacheIndex(x + offset.getX() , y + offset.getY(), z + offset.getZ());
			final boolean cornerClear = !isOpaque(cacheIndex);
			fd.topRight = cornerClear ? brightness(cacheIndex) : OPAQUE;
			fd.aoTopRight = (Math.round(ao(cacheIndex) * 255) + aoTop + aoCenter + aoRight + 1) >> 2;
		}

		fd.calc.compute(fd);
	}
}

