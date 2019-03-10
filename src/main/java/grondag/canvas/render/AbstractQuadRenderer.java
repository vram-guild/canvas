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

import grondag.canvas.RenderMaterialImpl;
import grondag.canvas.aocalc.AoCalculator;
import grondag.canvas.core.CompoundBufferBuilder;
import grondag.canvas.core.VertexCollector;
import grondag.canvas.helper.ColorHelper;
import grondag.canvas.mesh.MutableQuadViewImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.fabricmc.fabric.api.client.model.fabric.RenderContext.QuadTransform;
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
        ColorHelper.colorizeQuad(q, blockColorIndex == -1 ? -1 : (blockInfo.blockColor(blockColorIndex) | 0xFF000000));
    }

    /** final output step, common to all renders */
    private void bufferQuad(MutableQuadViewImpl q, int renderLayer) {
        final RenderMaterialImpl.Value mat = q.material().forRenderLayer(renderLayer);
        final VertexCollector output = bufferFunc.get(mat.renderLayerIndex).getVertexCollector(mat.pipeline);
        final int shaderFlags = mat.shaderFlags() << 16;
        
        for(int i = 0; i < 4; i++) {
            output.pos(blockInfo.blockPos, q.x(i), q.y(i), q.z(i));
            output.add(q.spriteColor(i, 0));
            output.add(q.spriteU(i, 0));
            output.add(q.spriteV(i, 0));
            int packedLight = q.lightmap(i);
            int blockLight = (packedLight & 0xFF);
            int skyLight = ((packedLight >> 16) & 0xFF);
            output.add(blockLight | (skyLight << 8) | shaderFlags);
            int ao = mat.hasAo ? ((Math.round(aoCalc.ao[i] * 254) - 127) << 24) : 0xFF000000;
            output.add(q.packedNormal(i) | ao);
            
            //TODO: output layers 2-3
        }
    }
    
    /** for non-emissive mesh quads and all fallback quads with smooth lighting */
    protected void tesselateSmooth(MutableQuadViewImpl q, int renderLayer, int blockColorIndex) {
        colorizeQuad(q, blockColorIndex);
        for (int i = 0; i < 4; i++) {
            q.lightmap(i, ColorHelper.maxBrightness(q.lightmap(i), aoCalc.light[i]));
        }
        bufferQuad(q, renderLayer);
    }

    /** for non-emissive mesh quads and all fallback quads with flat lighting */
    protected void tesselateFlat(MutableQuadViewImpl quad, int renderLayer, int blockColorIndex) {
        colorizeQuad(quad, blockColorIndex);
        final int brightness = flatBrightness(quad, blockInfo.blockState, blockInfo.blockPos);
        for (int i = 0; i < 4; i++) {
            quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), brightness));
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
