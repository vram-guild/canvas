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
import static grondag.canvas.varia.BlockPosHelper.fastFaceOffset;

import java.util.function.ToIntFunction;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;
import grondag.canvas.light.AoFace.Vertex2Float;
import grondag.canvas.light.AoFace.WeightFunction;
import grondag.canvas.varia.BlockPosHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ExtendedBlockView;

/**
 * Adaptation of inner, non-static class in BlockModelRenderer that serves same
 * purpose.
 */
@Environment(EnvType.CLIENT)
public class AoCalculator {
    public static final float DIVIDE_BY_255 = 1f / 255f;
    
    //PERF: could be better - or wait for a diff Ao model
    static final int BLEND_CACHE_DIVISION = 16;
    static final int BLEND_CACHE_DEPTH = BLEND_CACHE_DIVISION - 1;
    static final int BLEND_CACHE_ARRAY_SIZE = BLEND_CACHE_DEPTH * 6;
    static final int BLEND_INDEX_NO_DEPTH = -1;
    static final int BLEND_INDEX_FULL_DEPTH = BLEND_CACHE_DIVISION - 1;
    
    static int blendIndex(int face, float depth) {
        int depthIndex = MathHelper.clamp((((int)(depth * BLEND_CACHE_DIVISION * 2 + 1)) >> 1), 1, 15) - 1;
        return face * BLEND_CACHE_DEPTH + depthIndex;
    }
    
    /** Used to receive a method reference in constructor for ao value lookup. */
    @FunctionalInterface
    public static interface AoFunc {
        float apply(BlockPos pos);
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
        if(completionFlags != 0) {
            completionFlags = 0;
            for(int i = 0; i < BLEND_CACHE_ARRAY_SIZE; i++) {
                AoFaceCalc d = blendCache[i];
                if(d != null) {
                    d.release();
                    blendCache[i] = null;
                }
            }
        }
    }
    
    public void compute(MutableQuadViewImpl quad) {
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
        final int lightFace = quad.lightFace().ordinal();
        final AoFaceCalc faceData = gatherFace(lightFace, isOnLightFace).calc();
        final AoFace face = AoFace.get(lightFace);
        final WeightFunction wFunc = face.weightFunc;
        for (int i = 0; i < 4; i++) {
            final float[] w = quad.w[i];
            wFunc.apply(quad, i, w);
            light[i] = faceData.weightedCombinedLight(w);
            ao[i] = faceData.weigtedAo(w) * DIVIDE_BY_255;
        }
    }
    
    private void vanillaPartialFaceSmooth(MutableQuadViewImpl quad, boolean isOnLightFace) {
        final int lightFace = quad.lightFace().ordinal();
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
    private AoFaceCalc blendedInsetData(QuadViewImpl quad, int vertexIndex, int lightFace) {
        final float w1 = AoFace.get(lightFace).depthFunc.apply(quad, vertexIndex);
        if (w1 <= 0.03125f) {
            return gatherFace(lightFace, true).calc();
        } else if (w1 >= 0.96875f) {
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

    private void blendedFace(MutableQuadViewImpl quad) {
        final int lightFace = quad.lightFaceId();
        AoFaceCalc faceData = blendedInsetData(quad, 0, lightFace);
        AoFace face = AoFace.get(lightFace);
        final WeightFunction wFunc = face.weightFunc;
        for (int i = 0; i < 4; i++) {
            final float[] w = quad.w[i];
            wFunc.apply(quad, i, w);
            light[i] = faceData.weightedCombinedLight(w);
            ao[i] = faceData.weigtedAo(w) * DIVIDE_BY_255;;
        }
    }

    private void blendedPartialFaceSmooth(MutableQuadViewImpl quad) {
        final int lightFace = quad.lightFaceId();
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
        final float aoResult[] = this.ao;
        final int[] lightResult = this.light;

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
            if (!MathHelper.equalsApproximate(0f, x)) {
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
            if (!MathHelper.equalsApproximate(0f, y)) {
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
            if (!MathHelper.equalsApproximate(0f, z)) {
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
            lightResult[i] = (((int) ((sky + maxSky) * 0.5f) & 0xFF) << 16)
                    | ((int)((block + maxBlock) * 0.5f) & 0xFF);
        }
    }
    
    private static final int BOTTOM = 0;
    private static final int TOP = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;
    
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
            fd.resetCalc();
            
            final ExtendedBlockView world = blockInfo.blockView;
            final BlockPos pos = blockInfo.blockPos;
            final BlockPos.Mutable centerPos = this.lightPos;
            final BlockPos.Mutable searchPos = this.searchPos;

            // Overall this is different from vanilla, which seems to be buggy
            // basically, use neighbor pos unless it is full opaque - in that case cheat and use
            // this block's position.
            // A key difference from vanilla is that this position is then used as the center for 
            // all following offsets, which avoids anisotropy in smooth lighting.
            if(isOnBlockFace) {
                BlockPosHelper.fastFaceOffset(centerPos, pos, lightFace);
                if(world.getBlockState(centerPos).isFullOpaque(world, centerPos)) {
                    centerPos.set(pos);
                }
            } else {
                centerPos.set(pos);
            }
            fd.center = brightnessFunc.applyAsInt(centerPos);
            int aoCenter = Math.round(aoFunc.apply(centerPos) * 255);

            AoFace aoFace = AoFace.get(lightFace);
            
            // vanilla was further offsetting these in the direction of the light face
            // but it was actually mis-sampling and causing visible artifacts in certain situation
            // PERF: use clearness cache in chunk info
            fastFaceOffset(searchPos, centerPos, aoFace.neighbors[BOTTOM]);
            final boolean bottomClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
            fd.bottom = bottomClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
            int aoBottom = Math.round(aoFunc.apply(searchPos) * 255);
            
            fastFaceOffset(searchPos, centerPos, aoFace.neighbors[TOP]);
            final boolean topClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
            fd.top = topClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
            int aoTop = Math.round(aoFunc.apply(searchPos) * 255);
            
            fastFaceOffset(searchPos, centerPos, aoFace.neighbors[LEFT]);
            final boolean leftClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
            fd.left = leftClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
            int aoLeft = Math.round(aoFunc.apply(searchPos) * 255);
            
            fastFaceOffset(searchPos, centerPos, aoFace.neighbors[RIGHT]);
            final boolean rightClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
            fd.right = rightClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
            int aoRight = Math.round(aoFunc.apply(searchPos) * 255);

            if (!(leftClear || bottomClear)) { 
                // both not clear
            	fd.aoBottomLeft = (Math.min(aoLeft, aoBottom) + aoBottom + aoLeft + 1 + aoCenter) >> 2;
                fd.bottomLeft = OPAQUE;
            } else { // at least one clear
                fastFaceOffset(searchPos, fastFaceOffset(searchPos, centerPos, aoFace.neighbors[BOTTOM]), aoFace.neighbors[LEFT]);
                boolean cornerClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
                fd.bottomLeft = cornerClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
                fd.aoBottomLeft = (Math.round(aoFunc.apply(searchPos) * 255) + aoBottom + aoCenter + aoLeft + 1) >> 2;  // bitwise divide by four, rounding up
            }
            
            if (!(rightClear || bottomClear)) { 
                // both not clear
                fd.aoBottomRight = (Math.min(aoRight, aoBottom) + aoBottom + aoRight + 1 + aoCenter) >> 2;
                fd.bottomRight = OPAQUE;
            } else { // at least one clear
                fastFaceOffset(searchPos, fastFaceOffset(searchPos, centerPos, aoFace.neighbors[BOTTOM]), aoFace.neighbors[RIGHT]);
                boolean cornerClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
                fd.bottomRight = cornerClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
                fd.aoBottomRight = (Math.round(aoFunc.apply(searchPos) * 255) + aoBottom + aoCenter + aoRight + 1) >> 2;
            }
            
            if (!(leftClear || topClear)) { 
                // both not clear
                fd.aoTopLeft = (Math.min(aoLeft, aoTop) + aoTop + aoLeft + 1 + aoCenter) >> 2;
                fd.topLeft = OPAQUE;
            } else { // at least one clear
                fastFaceOffset(searchPos, fastFaceOffset(searchPos, centerPos, aoFace.neighbors[TOP]), aoFace.neighbors[LEFT]);
                boolean cornerClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
                fd.topLeft = cornerClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
                fd.aoTopLeft = (Math.round(aoFunc.apply(searchPos) * 255) + aoTop + aoCenter + aoLeft + 1) >> 2;
            }
            
            if (!(rightClear || topClear)) { 
                // both not clear
                fd.aoTopRight = (Math.min(aoRight, aoTop) + aoTop+ aoRight + 1 + aoCenter) >> 2;
                fd.topRight = OPAQUE;
            } else { // at least one clear
                fastFaceOffset(searchPos, fastFaceOffset(searchPos, centerPos, aoFace.neighbors[TOP]), aoFace.neighbors[RIGHT]);
                boolean cornerClear = !world.getBlockState(searchPos).isFullOpaque(world, searchPos);
                fd.topRight = cornerClear ? brightnessFunc.applyAsInt(searchPos) : OPAQUE;
                fd.aoTopRight = (Math.round(aoFunc.apply(searchPos) * 255) + aoTop + aoCenter + aoRight + 1) >> 2;
            }
        }
        return fd;
    }
}
