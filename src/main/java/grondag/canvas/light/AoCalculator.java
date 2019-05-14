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

package grondag.canvas.light;

import static grondag.canvas.apiimpl.util.GeometryHelper.AXIS_ALIGNED_FLAG;
import static grondag.canvas.apiimpl.util.GeometryHelper.CUBIC_FLAG;
import static grondag.canvas.apiimpl.util.GeometryHelper.LIGHT_FACE_FLAG;
import static grondag.canvas.light.AoFaceData.OPAQUE;
import static net.minecraft.util.math.Direction.DOWN;
import static net.minecraft.util.math.Direction.EAST;
import static net.minecraft.util.math.Direction.NORTH;
import static net.minecraft.util.math.Direction.SOUTH;
import static net.minecraft.util.math.Direction.UP;
import static net.minecraft.util.math.Direction.WEST;

import java.util.function.ToIntFunction;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;
import grondag.canvas.light.AoFace.Vertex2Float;
import grondag.canvas.light.AoFace.WeightFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
    
    //PERF: could be better - or wait for a diff Ao model
    static final int BLEND_CACHE_DIVISION = 16;
    static final int BLEND_CACHE_DEPTH = BLEND_CACHE_DIVISION - 2;
    static final int BLEND_CACHE_ARRAY_SIZE = BLEND_CACHE_DIVISION * 6;
    static final int BLEND_INDEX_NO_DEPTH = -1;
    static final int BLEND_INDEX_FULL_DEPTH = BLEND_CACHE_DIVISION - 1;
    
    static int blendIndex(Direction face, float depth) {
        return face.ordinal() * BLEND_CACHE_DEPTH + (((int)(depth * BLEND_CACHE_DIVISION * 2 + 1)) >> 1) - 1;
    }
    
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
    private final ToIntFunction<BlockPos> brightnessFunc;
    private final AoFunc aoFunc;
    
    private final AoFaceCalc[] blendCache = new AoFaceCalc[BLEND_CACHE_ARRAY_SIZE];
    
    // PERF: need to cache these vs only the calc results due to mixed use
    private final AoFaceData blender = new AoFaceData();
    
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

    // outputs
    public final float[] ao = new float[4];
    public final int[] light = new int[4];

    public AoCalculator(BlockRenderInfo blockInfo, ToIntFunction<BlockPos> brightnessFunc, AoFunc aoFunc) {
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
        for(int i = 0; i < BLEND_CACHE_ARRAY_SIZE; i++) {
            AoFaceCalc d = blendCache[i];
            if(d != null) {
                d.release();
                blendCache[i] = null;
            }
        }
    }

    /** returns true if should match vanilla results */
    public void compute(MutableQuadViewImpl quad) {
        final int flags = quad.geometryFlags();
        if(Configurator.enableHdLightmaps) {
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
        gatherFace(lightFace, isOnLightFace).calc().toArray(ao, light, VERTEX_MAP[lightFace.getId()]);
    }

    private void vanillaPartialFace(MutableQuadViewImpl quad, boolean isOnLightFace) {
        final Direction lightFace = quad.lightFace();
        final AoFaceCalc faceData = gatherFace(lightFace, isOnLightFace).calc();
        final AoFace face = AoFace.get(lightFace);
        final WeightFunction wFunc = face.weightFunc;
        for (int i = 0; i < 4; i++) {
            final float[] w = quad.w[i];
            wFunc.apply(quad, i, w);
            light[i] = faceData.weightedCombinedLight(w);
            ao[i] = faceData.weigtedAo(w);
        }
    }
    
    private void vanillaPartialFaceSmooth(MutableQuadViewImpl quad, boolean isOnLightFace) {
        final Direction lightFace = quad.lightFace();
        AoFaceData faceData = gatherFace(lightFace, isOnLightFace);
        AoFace face = AoFace.get(lightFace);
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
    private AoFaceCalc blendedInsetData(QuadViewImpl quad, int vertexIndex, Direction lightFace) {
        final float w1 = AoFace.get(lightFace).depthFunc.apply(quad, vertexIndex);
        if (MathHelper.equalsApproximate(w1, 0)) {
            return gatherFace(lightFace, true).calc();
        } else if (MathHelper.equalsApproximate(w1, 1)) {
            return gatherFace(lightFace, false).calc();
        } else {
            int depth = blendIndex(lightFace, w1);
            AoFaceCalc result = blendCache[depth];
            if(result == null) {
                final float w0 = 1 - w1;
                result = AoFaceCalc.weightedMean(
                        gatherFace(lightFace, true).calc(), w0, 
                        gatherFace(lightFace, false).calc(), w1);
                blendCache[depth] = result;
            }
            return result;
        }
    }

    private void blendedFullFace(MutableQuadViewImpl quad) {
        final Direction lightFace = quad.lightFace();
        blendedInsetData(quad, 0, lightFace).toArray(ao, light, VERTEX_MAP[lightFace.getId()]);
    }

    private void blendedPartialFace(MutableQuadViewImpl quad) {
        final Direction lightFace = quad.lightFace();
        AoFaceCalc faceData = blendedInsetData(quad, 0, lightFace);
        AoFace face = AoFace.get(lightFace);
        final WeightFunction wFunc = face.weightFunc;
        for (int i = 0; i < 4; i++) {
            final float[] w = quad.w[i];
            wFunc.apply(quad, i, w);
            light[i] = faceData.weightedCombinedLight(w);
            ao[i] = faceData.weigtedAo(w);
        }
    }

    private void blendedPartialFaceSmooth(MutableQuadViewImpl quad) {
        final Direction lightFace = quad.lightFace();
        
        final float w1 = AoFace.get(lightFace).depthFunc.apply(quad, 0);
        final float w0 = 1 - w1;
        // PERF: cache recent results somehow
        AoFaceData faceData = AoFaceData.weightedBlend(gatherFace(lightFace, true), w0, gatherFace(lightFace, false), w1, blender);
        AoFace face = AoFace.get(lightFace);
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
        quad.aoShade = null;
        
        for (int i = 0; i < 4; i++) {
            normal = quad.hasNormal(i) ? quad.copyNormal(i, vertexNormal) : faceNorm;
            float ao = 0, sky = 0, block = 0;
            int maxSky = 0, maxBlock = 0, maxAo = 0;

            final float x = normal.x();
            if (!MathHelper.equalsApproximate(0f, x)) {
                final Direction face = x > 0 ? Direction.EAST : Direction.WEST;
                // PERF: really need to cache these
                final AoFaceCalc fd = blendedInsetData(quad, i, face);
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
                final AoFaceCalc fd = blendedInsetData(quad, i, face);
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
                final AoFaceCalc fd = blendedInsetData(quad, i, face);
                AoFace.get(face).weightFunc.apply(quad, i, w);
                final float n = z * z;
                ao += n * fd.weigtedAo(w);
                sky += n * fd.weigtedSkyLight(w);
                block += n * fd.weigtedBlockLight(w);
                maxAo = fd.maxAo(maxAo);
                maxSky = fd.maxSkyLight(maxSky);
                maxBlock = fd.maxBlockLight(maxBlock);
            }

            aoResult[i] = (ao + maxAo) * (0.5f / 255f);
            lightResult[i] = (((int) ((sky + maxSky) * 0.5f) & 0xFF) << 16)
                    | ((int) ((block + maxBlock) * 0.5f) & 0xFF);
        }
    }
    
    private static final int BOTTOM = 0;
    private static final int TOP = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;
    
    private static final int OPAQUE_AO = Math.round(0.2f * 255);
    
    /**
     * Computes smoothed brightness and Ao shading for four corners of a block face.
     * Outer block face is what you normally see and what you get get when second
     * parameter is true. Inner is light *within* the block and usually darker. It
     * is blended with the outer face for inset surfaces, but is also used directly
     * in vanilla logic for some blocks that aren't full opaque cubes. Except for
     * parameterization, the logic itself is practically identical to vanilla.
     */
    private AoFaceData gatherFace(Direction lightFace, boolean isOnBlockFace) {
        final int faceDataIndex = isOnBlockFace ? lightFace.getId() : lightFace.getId() + 6;
        final int mask = 1 << faceDataIndex;
        final AoFaceData fd = faceData[faceDataIndex];
        if ((completionFlags & mask) == 0) {
            completionFlags |= mask;
            fd.resetCalc();
            
            final ExtendedBlockView world = blockInfo.blockView;
            final BlockPos pos = blockInfo.blockPos;
            final BlockPos.Mutable lightPos = this.lightPos;
            final BlockPos.Mutable searchPos = this.searchPos;

            // PERF: don't generate instance here
            lightPos.set(isOnBlockFace ? pos.offset(lightFace) : pos);
            AoFace aoFace = AoFace.get(lightFace);

            // If on block face or neighbor isn't occluding, "center" will be neighbor
            // brightness
            // Doesn't use light pos because logic not based solely on this block's geometry
            searchPos.set((Vec3i) pos).setOffset(lightFace);
            if (isOnBlockFace || !world.getBlockState(searchPos).isFullOpaque(world, searchPos)) {
                fd.center = brightnessFunc.applyAsInt(searchPos);
            } else {
                fd.center = brightnessFunc.applyAsInt(pos);
            }

            int aoCenter = Math.round(aoFunc.apply(isOnBlockFace ? lightPos : pos) * 255);
            
            // vanilla was further offsetting these in the direction of the light face
            // but it was actually mis-sampling and causing visible artifacts in certain situation
            // PERF: use clearness cache in chunk info
            searchPos.set(lightPos).setOffset(aoFace.neighbors[BOTTOM]);
            final boolean bottomClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
            fd.bottom = bottomClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
            int aoBottom = bottomClear ? Math.round(aoFunc.apply(searchPos) * 255) : OPAQUE_AO;
            
            searchPos.set(lightPos).setOffset(aoFace.neighbors[TOP]);
            final boolean topClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
            fd.top = topClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
            int aoTop = topClear ? Math.round(aoFunc.apply(searchPos) * 255) : OPAQUE_AO;
            
            searchPos.set(lightPos).setOffset(aoFace.neighbors[LEFT]);
            final boolean leftClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
            fd.left = leftClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
            int aoLeft = leftClear ? Math.round(aoFunc.apply(searchPos) * 255) : OPAQUE_AO;
            
            searchPos.set(lightPos).setOffset(aoFace.neighbors[RIGHT]);
            final boolean rightClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
            fd.right = rightClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
            int aoRight = rightClear ? Math.round(aoFunc.apply(searchPos) * 255) : OPAQUE_AO;

            if (!(leftClear || bottomClear)) { 
                // both not clear
                fd.aoBottomLeft = (OPAQUE_AO + OPAQUE_AO + OPAQUE_AO + 1 + aoCenter) >> 2;
                fd.bottomLeft = OPAQUE;
            } else { // at least one clear
                searchPos.set(lightPos).setOffset(aoFace.neighbors[BOTTOM]).setOffset(aoFace.neighbors[LEFT]);
                boolean cornerClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
                fd.bottomLeft = cornerClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
                final int aoBottomLeft = aoLookup(world, cornerClear, lightFace, aoFace, LEFT, BOTTOM);
                fd.aoBottomLeft = (aoBottomLeft + aoBottom + aoCenter + aoLeft + 1) >> 2;  // bitwise divide by four, rounding up
            }
            
            if (!(rightClear || bottomClear)) { 
                // both not clear
                fd.aoBottomRight = (OPAQUE_AO + OPAQUE_AO + OPAQUE_AO + 1 + aoCenter) >> 2;
                fd.bottomRight = OPAQUE;
            } else { // at least one clear
                searchPos.set(lightPos).setOffset(aoFace.neighbors[BOTTOM]).setOffset(aoFace.neighbors[RIGHT]);
                boolean cornerClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
                fd.bottomRight = cornerClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
                final int aoBottomRight = aoLookup(world, cornerClear, lightFace, aoFace, RIGHT, BOTTOM);
                fd.aoBottomRight = (aoBottomRight + aoBottom + aoCenter + aoRight + 1) >> 2;
            }
            
            if (!(leftClear || topClear)) { 
                // both not clear
                fd.aoTopLeft = (OPAQUE_AO + OPAQUE_AO + OPAQUE_AO + 1 + aoCenter) >> 2;
                fd.topLeft = OPAQUE;
            } else { // at least one clear
                searchPos.set(lightPos).setOffset(aoFace.neighbors[TOP]).setOffset(aoFace.neighbors[LEFT]);
                boolean cornerClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
                fd.topLeft = cornerClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
                final int aoTopLeft = aoLookup(world, cornerClear, lightFace, aoFace, TOP, LEFT);
                fd.aoTopLeft = (aoTopLeft + aoTop + aoCenter + aoLeft + 1) >> 2;
            }
            
            if (!(rightClear || topClear)) { 
                // both not clear
                fd.aoTopRight = (OPAQUE_AO + OPAQUE_AO + OPAQUE_AO + 1 + aoCenter) >> 2;
                fd.topRight = OPAQUE;
            } else { // at least one clear
                searchPos.set(lightPos).setOffset(aoFace.neighbors[TOP]).setOffset(aoFace.neighbors[RIGHT]);
                boolean cornerClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
                fd.topRight = cornerClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
                final int aoTopRight = aoLookup(world, cornerClear, lightFace, aoFace, TOP, RIGHT);
                fd.aoTopRight = (aoTopRight + aoTop + aoCenter + aoRight + 1) >> 2;
            }
        }
        return fd;
    }
    
    //TODO: make configurable or remove if abandoned
    static final boolean AO_TWEAK = true;
    
    private int aoLookup(ExtendedBlockView world, boolean cornerClear, Direction lightFace, AoFace aoFace, int uIndex, int vIndex) {
//        if(AO_TWEAK) {
//            if(cornerClear) {
//                return Math.round(aoFunc.apply(searchPos) * 255);
//            } else {
//                // if block next to this block is open, use it
//                searchPos.set(lightPos).setOffset(aoFace.neighbors[vIndex]).setOffset(lightFace.getOpposite());
//                if(!world.getBlockState(searchPos).isFullOpaque(world, searchPos)) {
//                    return Math.round(aoFunc.apply(searchPos) * 255);
//                } else {
//                    searchPos.set(lightPos).setOffset(aoFace.neighbors[uIndex]).setOffset(lightFace.getOpposite());
//                    if(!world.getBlockState(searchPos).isFullOpaque(world, searchPos)) {
//                        return Math.round(aoFunc.apply(searchPos) * 255);
//                    } else {
//                        return OPAQUE_AO;
//                    }
//                }
//            }
//        } else {
            return cornerClear ? Math.round(aoFunc.apply(searchPos) * 255) : OPAQUE_AO;
//        }
    }
}
