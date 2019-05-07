/*******************************************************************************
 * Copyright 2019 grondag
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
 ******************************************************************************/

package grondag.canvas.apiimpl.util;

import static grondag.canvas.apiimpl.util.GeometryHelper.AXIS_ALIGNED_FLAG;
import static grondag.canvas.apiimpl.util.GeometryHelper.CUBIC_FLAG;
import static grondag.canvas.apiimpl.util.GeometryHelper.LIGHT_FACE_FLAG;
import static net.minecraft.util.math.Direction.DOWN;
import static net.minecraft.util.math.Direction.EAST;
import static net.minecraft.util.math.Direction.NORTH;
import static net.minecraft.util.math.Direction.SOUTH;
import static net.minecraft.util.math.Direction.UP;
import static net.minecraft.util.math.Direction.WEST;

import java.util.function.ToIntBiFunction;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;
import grondag.canvas.apiimpl.util.AoFace.Vertex2Float;
import grondag.canvas.apiimpl.util.AoFace.WeightFunction;
import grondag.canvas.varia.LightmapHD;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ExtendedBlockView;

/**
 * Adaptation of inner, non-static class in BlockModelRenderer that serves same
 * purpose.
 */
@Environment(EnvType.CLIENT)
public class AoCalculator {
    /** Used to receive a method reference in constructor for ao value lookup. */
    @FunctionalInterface
    public static interface AoFunc {
        float apply(BlockPos pos);
    }

    /**
     * Vanilla models with cubic quads have vertices in a certain order, which
     * allows us to map them using a lookup. Adapted from enum in vanilla
     * AoCalculator.
     */
    private static final int[][] VERTEX_MAP = new int[6][4];
    static {
        VERTEX_MAP[DOWN.getId()] = new int[] { 0, 1, 2, 3 };
        VERTEX_MAP[UP.getId()] = new int[] { 2, 3, 0, 1 };
        VERTEX_MAP[NORTH.getId()] = new int[] { 3, 0, 1, 2 };
        VERTEX_MAP[SOUTH.getId()] = new int[] { 0, 1, 2, 3 };
        VERTEX_MAP[WEST.getId()] = new int[] { 3, 0, 1, 2 };
        VERTEX_MAP[EAST.getId()] = new int[] { 1, 2, 3, 0 };
    }

    private final BlockPos.Mutable lightPos = new BlockPos.Mutable();
    private final BlockPos.Mutable searchPos = new BlockPos.Mutable();
    private final BlockRenderInfo blockInfo;
    private final ToIntBiFunction<BlockState, BlockPos> brightnessFunc;
    private final AoFunc aoFunc;

    /**
     * caches results of {@link #computeFace(Direction, boolean)} for the current
     * block
     */
    private final AoFaceData[] faceData = new AoFaceData[12];

    /**
     * indicates which elements of {@link #faceData} have been computed for the
     * current block
     */
    private int completionFlags = 0;

    // outputs
    public final float[] ao = new float[4];
    public final int[] light = new int[4];

    public AoCalculator(BlockRenderInfo blockInfo, ToIntBiFunction<BlockState, BlockPos> brightnessFunc, AoFunc aoFunc) {
        this.blockInfo = blockInfo;
        this.brightnessFunc = brightnessFunc;
        this.aoFunc = aoFunc;
        for (int i = 0; i < 12; i++) {
            faceData[i] = new AoFaceData();
        }
    }

    /** call at start of each new block */
    public void clear() {
        completionFlags = 0;
    }

    /** returns true if should match vanilla results */
    public void compute(MutableQuadViewImpl quad) {
        final int flags = quad.geometryFlags();
        if(Configurator.enableSmoothLightmaps) {
            if((flags & AXIS_ALIGNED_FLAG) == AXIS_ALIGNED_FLAG) {
                if((flags & LIGHT_FACE_FLAG) == LIGHT_FACE_FLAG) {
                    vanillaPartialFace(quad, true);
                } else {
                    blendedPartialFace(quad);
                }
            } else {
                // currently can't handle these
                irregularFace(quad);
            }
            return;
        }
        
        switch (flags) {
        case AXIS_ALIGNED_FLAG | CUBIC_FLAG | LIGHT_FACE_FLAG:
            vanillaFullFace(quad, true);
            break;

        case AXIS_ALIGNED_FLAG | LIGHT_FACE_FLAG:
            vanillaPartialFace(quad, true);
            break;

        case AXIS_ALIGNED_FLAG | CUBIC_FLAG:
            blendedFullFace(quad);
            break;

        case AXIS_ALIGNED_FLAG:
            blendedPartialFace(quad);
            break;

        default:
            irregularFace(quad);
            break;
        }
    }

    private void vanillaFullFace(MutableQuadViewImpl quad, boolean isOnLightFace) {
        final Direction lightFace = quad.lightFace();
        computeFace(lightFace, isOnLightFace).toArray(ao, light, VERTEX_MAP[lightFace.getId()]);
    }

    private void vanillaPartialFace(MutableQuadViewImpl quad, boolean isOnLightFace) {
        final Direction lightFace = quad.lightFace();
        AoFaceData faceData = computeFace(lightFace, isOnLightFace);
        AoFace face = AoFace.get(lightFace);
        final WeightFunction wFunc = face.weightFunc;
        final Vertex2Float uFunc = face.uFunc;
        final Vertex2Float vFunc = face.vFunc;
        for (int i = 0; i < 4; i++) {
            final float[] w = quad.w[i];
            wFunc.apply(quad, i, w);
            quad.u[i] = uFunc.apply(quad, i);
            quad.v[i] = vFunc.apply(quad, i);
            light[i] = faceData.weightedCombinedLight(w);
            ao[i] = faceData.weigtedAo(w);
        }
        
        //PERF: only add these if extra smooth lighting enabled
        quad.shadeFaceData = ShadeFaceData.find(faceData);
        quad.blockLight = LightmapHD.findBlock(faceData);
        quad.skyLight = LightmapHD.findSky(faceData);
    }

    /**
     * used in {@link #blendedInsetFace(VertexEditorImpl, Direction)} as return
     * variable to avoid new allocation
     */
    AoFaceData tmpFace = new AoFaceData();
    

    

    /**
     * Returns linearly interpolated blend of outer and inner face based on depth of
     * vertex in face
     */
    private AoFaceData blendedInsetData(QuadViewImpl quad, int vertexIndex, Direction lightFace) {
        final float w1 = AoFace.get(lightFace).depthFunc.apply(quad, vertexIndex);
        final float w0 = 1 - w1;
        return AoFaceData.weightedMean(computeFace(lightFace, true), w0, computeFace(lightFace, false), w1, tmpFace);
    }

    /**
     * Like {@link #blendedInsetFace(VertexEditorImpl, Direction)} but optimizes if
     * depth is 0 or 1. Used for irregular faces when depth varies by vertex to
     * avoid unneeded interpolation.
     */
    private AoFaceData fastInsetData(QuadViewImpl quad, int vertexIndex, Direction lightFace) {
        final float w1 = AoFace.get(lightFace).depthFunc.apply(quad, vertexIndex);
        if (MathHelper.equalsApproximate(w1, 0)) {
            return computeFace(lightFace, true);
        } else if (MathHelper.equalsApproximate(w1, 1)) {
            return computeFace(lightFace, false);
        } else {
            final float w0 = 1 - w1;
            return AoFaceData.weightedMean(computeFace(lightFace, true), w0, computeFace(lightFace, false), w1,
                    tmpFace);
        }
    }

    private void blendedFullFace(MutableQuadViewImpl quad) {
        final Direction lightFace = quad.lightFace();
        blendedInsetData(quad, 0, lightFace).toArray(ao, light, VERTEX_MAP[lightFace.getId()]);
    }

    private void blendedPartialFace(MutableQuadViewImpl quad) {
        final Direction lightFace = quad.lightFace();
        AoFaceData faceData = blendedInsetData(quad, 0, lightFace);
        AoFace face = AoFace.get(lightFace);
        final WeightFunction wFunc = face.weightFunc;
        final Vertex2Float uFunc = face.uFunc;
        final Vertex2Float vFunc = face.vFunc;
        for (int i = 0; i < 4; i++) {
            final float[] w = quad.w[i];
            wFunc.apply(quad, i, w);
            quad.u[i] = uFunc.apply(quad, i);
            quad.v[i] = vFunc.apply(quad, i);
            light[i] = faceData.weightedCombinedLight(w);
            ao[i] = faceData.weigtedAo(w);
        }
        //PERF: only add these if extra smooth lighting enabled
        quad.shadeFaceData = ShadeFaceData.find(faceData);
        quad.skyLight = LightmapHD.findSky(faceData);
        quad.blockLight = LightmapHD.findBlock(faceData);
    }

    /**
     * used exclusively in irregular face to avoid new heap allocations each call.
     */
    private final Vector3f vertexNormal = new Vector3f();

    private final float[] w = new float[4];
    
    private void irregularFace(MutableQuadViewImpl quad) {
        final Vector3f faceNorm = quad.faceNormal();
        Vector3f normal;
        final float[] w = this.w;
        final float aoResult[] = this.ao;
        final int[] lightResult = this.light;

        //TODO: currently no way to handle 3d interpolation shader-side
        quad.blockLight = null;
        quad.skyLight = null;
        quad.shadeFaceData = null;
        
        for (int i = 0; i < 4; i++) {
            normal = quad.hasNormal(i) ? quad.copyNormal(i, vertexNormal) : faceNorm;
            float ao = 0, sky = 0, block = 0, maxAo = 0;
            int maxSky = 0, maxBlock = 0;

            final float x = normal.x();
            if (!MathHelper.equalsApproximate(0f, x)) {
                final Direction face = x > 0 ? Direction.EAST : Direction.WEST;
                final AoFaceData fd = fastInsetData(quad, i, face);
                AoFace.get(face).weightFunc.apply(quad, i, w);
                final float n = x * x;
                ao += n * fd.weigtedAo(w);
                sky += n * fd.weigtedSkyLight(w);
                block += n * fd.weigtedBlockLight(w);
                maxAo = fd.maxAo(maxAo);
                maxSky = fd.maxSkyLight(maxSky);
                maxBlock = fd.maxBlockLight(maxBlock);
            }

            final float y = normal.y();
            if (!MathHelper.equalsApproximate(0f, y)) {
                final Direction face = y > 0 ? Direction.UP : Direction.DOWN;
                final AoFaceData fd = fastInsetData(quad, i, face);
                AoFace.get(face).weightFunc.apply(quad, i, w);
                final float n = y * y;
                ao += n * fd.weigtedAo(w);
                sky += n * fd.weigtedSkyLight(w);
                block += n * fd.weigtedBlockLight(w);
                maxAo = fd.maxAo(maxAo);
                maxSky = fd.maxSkyLight(maxSky);
                maxBlock = fd.maxBlockLight(maxBlock);
            }

            final float z = normal.z();
            if (!MathHelper.equalsApproximate(0f, z)) {
                final Direction face = z > 0 ? Direction.SOUTH : Direction.NORTH;
                final AoFaceData fd = fastInsetData(quad, i, face);
                AoFace.get(face).weightFunc.apply(quad, i, w);
                final float n = z * z;
                ao += n * fd.weigtedAo(w);
                sky += n * fd.weigtedSkyLight(w);
                block += n * fd.weigtedBlockLight(w);
                maxAo = fd.maxAo(maxAo);
                maxSky = fd.maxSkyLight(maxSky);
                maxBlock = fd.maxBlockLight(maxBlock);
            }

            aoResult[i] = (ao + maxAo) * 0.5f;
            lightResult[i] = (((int) ((sky + maxSky) * 0.5f) & 0xF0) << 16)
                    | ((int) ((block + maxBlock) * 0.5f) & 0xF0);
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
    private AoFaceData computeFace(Direction lightFace, boolean isOnBlockFace) {
        final int faceDataIndex = isOnBlockFace ? lightFace.getId() : lightFace.getId() + 6;
        final int mask = 1 << faceDataIndex;
        final AoFaceData fd = faceData[faceDataIndex];
        if ((completionFlags & mask) == 0) {
            completionFlags |= mask;

            final ExtendedBlockView world = blockInfo.blockView;
            final BlockState blockState = blockInfo.blockState;
            final BlockPos pos = blockInfo.blockPos;
            final BlockPos.Mutable lightPos = this.lightPos;
            final BlockPos.Mutable searchPos = this.searchPos;

            // PERF: don't generate instance here
            lightPos.set(isOnBlockFace ? pos.offset(lightFace) : pos);
            AoFace aoFace = AoFace.get(lightFace);

            // PERF: make these lookups lazy - they may not get used if neighbor is obscured
            searchPos.set(lightPos).setOffset(aoFace.neighbors[0]);
            fd.light0 = brightnessFunc.applyAsInt(blockState, searchPos);
            fd.ao0 = aoFunc.apply(searchPos);
            searchPos.set(lightPos).setOffset(aoFace.neighbors[1]);
            fd.light1 = brightnessFunc.applyAsInt(blockState, searchPos);
            fd.ao1 = aoFunc.apply(searchPos);
            searchPos.set(lightPos).setOffset(aoFace.neighbors[2]);
            fd.light2 = brightnessFunc.applyAsInt(blockState, searchPos);
            fd.ao2 = aoFunc.apply(searchPos);
            searchPos.set(lightPos).setOffset(aoFace.neighbors[3]);
            fd.light3 = brightnessFunc.applyAsInt(blockState, searchPos);
            fd.ao3 = aoFunc.apply(searchPos);

            // vanilla was further offsetting these in the direction of the light face
            // but it was actually mis-sampling and causing visible artifacts in certain situation
            searchPos.set(lightPos).setOffset(aoFace.neighbors[0]);//.setOffset(lightFace);
            final boolean isClear0 = world.getBlockState(searchPos).getLightSubtracted(world, searchPos) == 0;
            searchPos.set(lightPos).setOffset(aoFace.neighbors[1]);//.setOffset(lightFace);
            final boolean isClear1 = world.getBlockState(searchPos).getLightSubtracted(world, searchPos) == 0;
            searchPos.set(lightPos).setOffset(aoFace.neighbors[2]);//.setOffset(lightFace);
            final boolean isClear2 = world.getBlockState(searchPos).getLightSubtracted(world, searchPos) == 0;
            searchPos.set(lightPos).setOffset(aoFace.neighbors[3]);//.setOffset(lightFace);
            final boolean isClear3 = world.getBlockState(searchPos).getLightSubtracted(world, searchPos) == 0;

            // If neighbors on both side of the corner are opaque, then apparently we use
            // the light/shade
            // from one of the sides adjacent to the corner. If either neighbor is clear (no
            // light subtraction)
            // then we use values from the outwardly diagonal corner. (outwardly = position
            // is one more away from light face)
            
            // Was probably an anisotropy problem here because we choose an arbitrary side
            // when both side are not clear and they do not have to have the same ao valuw.
            // Probably doesn't matter in vanilla where the only ao values are apparently 0.2 and 1.0.
            if (!isClear2 && !isClear0) {
                fd.cAo0 = (fd.ao0 + fd.ao2) * 0.5f;
                // vanilla picks one but we set to zero to force the "use min non-zero" behavior
                // and this give more consistent results
                fd.cLight0 = 0;
            } else {
                searchPos.set(lightPos).setOffset(aoFace.neighbors[0]).setOffset(aoFace.neighbors[2]);
                fd.cAo0 = aoFunc.apply(searchPos);
                fd.cLight0 = brightnessFunc.applyAsInt(blockState, searchPos);
            }

            if (!isClear3 && !isClear0) {
                fd.cAo1 = (fd.ao0 + fd.ao3) * 0.5f;
                fd.cLight1 = 0;
            } else {
                searchPos.set(lightPos).setOffset(aoFace.neighbors[0]).setOffset(aoFace.neighbors[3]);
                fd.cAo1 = aoFunc.apply(searchPos);
                fd.cLight1 = brightnessFunc.applyAsInt(blockState, searchPos);
            }

            if (!isClear2 && !isClear1) {
                fd.cAo2 = (fd.ao1 + fd.ao2) * 0.5f;
                fd.cLight2 = 0;
            } else {
                searchPos.set(lightPos).setOffset(aoFace.neighbors[1]).setOffset(aoFace.neighbors[2]);
                fd.cAo2 = aoFunc.apply(searchPos);
                fd.cLight2 = brightnessFunc.applyAsInt(blockState, searchPos);
            }

            if (!isClear3 && !isClear1) {
                fd.cAo3 = (fd.ao1 + fd.ao3) * 0.5f;
                fd.cLight3 = 0;
            } else {
                searchPos.set(lightPos).setOffset(aoFace.neighbors[1]).setOffset(aoFace.neighbors[3]);
                fd.cAo3 = aoFunc.apply(searchPos);
                fd.cLight3 = brightnessFunc.applyAsInt(blockState, searchPos);
            }

            // If on block face or neighbor isn't occluding, "center" will be neighbor
            // brightness
            // Doesn't use light pos because logic not based solely on this block's geometry
            searchPos.set((Vec3i) pos).setOffset(lightFace);
            if (isOnBlockFace || !world.getBlockState(searchPos).isFullOpaque(world, searchPos)) {
                fd.lightCenter = brightnessFunc.applyAsInt(blockState, searchPos);
            } else {
                fd.lightCenter = brightnessFunc.applyAsInt(blockState, pos);
            }

            fd.aoCenter = aoFunc.apply(isOnBlockFace ? lightPos : pos);
            
            fd.compute();
        }
        return fd;
    }
}
