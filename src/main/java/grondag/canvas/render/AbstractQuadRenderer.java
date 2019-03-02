/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grondag.canvas.render;

import static grondag.canvas.helper.GeometryHelper.LIGHT_FACE_FLAG;

import java.util.function.ToIntBiFunction;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.fabricmc.fabric.api.client.model.fabric.RenderContext.QuadTransform;
import grondag.canvas.aocalc.AoCalculator;
import grondag.canvas.core.CompoundBufferBuilder;
import grondag.canvas.core.PipelineManager;
import grondag.canvas.core.VertexCollector;
import grondag.canvas.helper.ColorHelper;
import grondag.canvas.mesh.MutableQuadViewImpl;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * Base quad-rendering class for fallback and mesh consumers. Has most of the
 * actual buffer-time lighting and coloring logic.
 */
public abstract class AbstractQuadRenderer {
    protected final ToIntBiFunction<BlockState, BlockPos> brightnessFunc;
    protected final Int2ObjectFunction<CompoundBufferBuilder> bufferFunc;
    protected final BlockRenderInfo blockInfo;
    protected final AoCalculator aoCalc;
    protected final QuadTransform transform;

    AbstractQuadRenderer(BlockRenderInfo blockInfo, ToIntBiFunction<BlockState, BlockPos> brightnessFunc,
            Int2ObjectFunction<CompoundBufferBuilder> bufferFunc, AoCalculator aoCalc, QuadTransform transform) {
        this.blockInfo = blockInfo;
        this.brightnessFunc = brightnessFunc;
        this.bufferFunc = bufferFunc;
        this.aoCalc = aoCalc;
        this.transform = transform;
    }

    /** handles block color and red-blue swizzle, common to all renders */
    private void colorizeQuad(MutableQuadViewImpl q, int blockColorIndex) {
        if (blockColorIndex == -1) {
            for (int i = 0; i < 4; i++) {
                q.spriteColor(i, 0, ColorHelper.swapRedBlueIfNeeded(q.spriteColor(i, 0)));
            }
        } else {
            final int blockColor = blockInfo.blockColor(blockColorIndex);
            for (int i = 0; i < 4; i++) {
                q.spriteColor(i, 0,
                        ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(blockColor, q.spriteColor(i, 0))));
            }
        }
    }

    /** final output step, common to all renders */
    private void bufferQuad(MutableQuadViewImpl q, int renderLayer) {
        // TODO: handle additional pipelines
        VertexCollector output = bufferFunc.get(renderLayer).getVertexCollector(PipelineManager.INSTANCE.defaultSinglePipeline);
        for(int i = 0; i < 4; i++) {
            output.pos(blockInfo.blockPos, q.x(i), q.y(i), q.z(i));
            output.add(q.spriteColor(i, 0));
            output.add(q.spriteU(i, 0));
            output.add(q.spriteV(i, 0));
            int packedLight = q.lightmap(0);
            int blockLight = ((packedLight >> 4) & 0xF) * 17;
            int skyLight = ((packedLight >> 20) & 0xF) * 17;
            output.add(blockLight | (skyLight << 8) | (encodeFlags(q, renderLayer) << 16));
            output.add(q.packedNormal(i));
        }
    }

    /**
     * Encode flags for emissive rendering, cutout and mipped handling.
     * This allows MC CUTOUT and CUTOUT_MIPPED quads to be backed into a single buffer
     * and rendered in the same draw command.  If cutout is on, then any fragment in
     * the base layer with a (base) texture alpha value less than 0.5 will be discarded.<p>
     * 
     * Layered quads don't generally use cutout textures, but if a model does supply
     * a base texture with holes and the quad is set to use a cutout layer, then the
     * discard will also affect overlay textures.  In other words, if the base texture has 
     * a hole,  the hole will not be covered by an overlay texture, even if the overlay is 
     * fully opaque.  (This could change in the future.)
     * 
     */
    private int encodeFlags(MutableQuadViewImpl q, int renderLayer)
    {
        // TODO: handle layers 1-2
        int result = q.material().emissive(0) ? 1 : 0;
                
        // mipped indicator
        // all are mipped except the one that isn't
        // TODO: handle multiple layers?
        if(renderLayer != BlockRenderLayer.MIPPED_CUTOUT.ordinal())
            result |= 0b00001000;
        
        // cutout indicator
        // PERF: sucks
        if(renderLayer == BlockRenderLayer.MIPPED_CUTOUT.ordinal() || renderLayer == BlockRenderLayer.CUTOUT.ordinal())
            result |= 0b00010000;
        
        return result;
    }
    
    // routines below have a bit of copy-paste code reuse to avoid conditional
    // execution inside a hot loop

    /** for non-emissive mesh quads and all fallback quads with smooth lighting */
    protected void tesselateSmooth(MutableQuadViewImpl q, int renderLayer, int blockColorIndex) {
        colorizeQuad(q, blockColorIndex);
        for (int i = 0; i < 4; i++) {
            q.spriteColor(i, 0, ColorHelper.multiplyRGB(q.spriteColor(i, 0), aoCalc.ao[i]));
            q.lightmap(i, aoCalc.light[i]);
        }
        bufferQuad(q, renderLayer);
    }

    /** for emissive mesh quads with smooth lighting */
    protected void tesselateSmoothEmissive(MutableQuadViewImpl q, int renderLayer, int blockColorIndex,
            int[] lightmaps) {
        colorizeQuad(q, blockColorIndex);
        for (int i = 0; i < 4; i++) {
            q.spriteColor(i, 0, ColorHelper.multiplyRGB(q.spriteColor(i, 0), aoCalc.ao[i]));
            q.lightmap(i, ColorHelper.maxBrightness(lightmaps[i], aoCalc.light[i]));
        }
        bufferQuad(q, renderLayer);
    }

    /** for non-emissive mesh quads and all fallback quads with flat lighting */
    protected void tesselateFlat(MutableQuadViewImpl quad, int renderLayer, int blockColorIndex) {
        colorizeQuad(quad, blockColorIndex);
        final int brightness = flatBrightness(quad, blockInfo.blockState, blockInfo.blockPos);
        for (int i = 0; i < 4; i++) {
            quad.lightmap(i, brightness);
        }
        bufferQuad(quad, renderLayer);
    }

    /** for emissive mesh quads with flat lighting */
    protected void tesselateFlatEmissive(MutableQuadViewImpl quad, int renderLayer, int blockColorIndex,
            int[] lightmaps) {
        colorizeQuad(quad, blockColorIndex);
        final int brightness = flatBrightness(quad, blockInfo.blockState, blockInfo.blockPos);
        for (int i = 0; i < 4; i++) {
            quad.lightmap(i, ColorHelper.maxBrightness(lightmaps[i], brightness));
        }
        bufferQuad(quad, renderLayer);
    }

    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    /**
     * Handles geometry-based check for using self brightness or neighbor
     * brightness. That logic only applies in flat lighting.
     */
    int flatBrightness(MutableQuadViewImpl quad, BlockState blockState, BlockPos pos) {
        mpos.set(pos);
        if ((quad.geometryFlags() & LIGHT_FACE_FLAG) != 0) {
            mpos.setOffset(quad.lightFace());
        }
        return brightnessFunc.applyAsInt(blockState, mpos);
    }
}
