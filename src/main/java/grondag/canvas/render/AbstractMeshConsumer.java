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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import grondag.canvas.RenderMaterialImpl;
import grondag.canvas.RenderMaterialImpl.Value;
import grondag.canvas.RendererImpl;
import grondag.canvas.aocalc.AoCalculator;
import grondag.canvas.core.VertexCollector;
import grondag.canvas.mesh.EncodingFormat;
import grondag.canvas.mesh.MeshImpl;
import grondag.canvas.mesh.MutableQuadViewImpl;
import grondag.frex.api.core.Mesh;
import grondag.frex.api.core.QuadEmitter;
import grondag.frex.api.core.RenderContext.QuadTransform;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * Consumer for pre-baked meshes. Works by copying the mesh data to a "editor"
 * quad held in the instance, where all transformations are applied before
 * buffering.
 */
public abstract class AbstractMeshConsumer extends AbstractQuadRenderer implements Consumer<Mesh> {
    protected AbstractMeshConsumer(BlockRenderInfo blockInfo, ToIntBiFunction<BlockState, BlockPos> brightnessFunc,
            Function<RenderMaterialImpl.Value, VertexCollector> collectorFunc, AoCalculator aoCalc, QuadTransform transform) {
        super(blockInfo, brightnessFunc, collectorFunc, aoCalc, transform);
        editorQuad = new Maker();
    }

    /**
     * Where we handle all pre-buffer coloring, lighting, transformation, etc.
     * Reused for all mesh quads. Fixed baking array sized to hold largest possible
     * mesh quad.
     */
    private class Maker extends MutableQuadViewImpl implements QuadEmitter {
        {
            data = new int[EncodingFormat.MAX_STRIDE];
            material = (Value) RendererImpl.INSTANCE.materialFinder().spriteDepth(RenderMaterialImpl.MAX_SPRITE_DEPTH)
                    .find();
        }

        // only used via RenderContext.getEmitter()
        @Override
        public Maker emit() {
            if (blockInfo.shouldDrawFace(this.cullFace())) {
                renderQuad();
            }
            clear();
            return this;
        }
    };

    @Override
    public void accept(Mesh mesh) {
        MeshImpl m = (MeshImpl) mesh;
        final int[] data = m.data();
        final int limit = data.length;
        int index = 0;
        while (index < limit) {
            RenderMaterialImpl.Value mat = RenderMaterialImpl.byIndex(data[index]);
            final int stride = EncodingFormat.stride(mat.spriteDepth());
            System.arraycopy(data, index, editorQuad.data(), 0, stride);
            editorQuad.load();
            index += stride;
            renderQuad();
        }
    }

    public QuadEmitter getEmitter() {
        editorQuad.clear();
        return editorQuad;
    }
}