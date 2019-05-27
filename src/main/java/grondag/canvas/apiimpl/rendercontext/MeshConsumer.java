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

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.MeshImpl;
import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.buffer.packing.VertexCollector;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.ShaderContext;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.minecraft.util.math.BlockPos;

/**
 * Consumer for pre-baked meshes. Works by copying the mesh data to a "editor"
 * quad held in the instance, where all transformations are applied before
 * buffering.
 */
public class MeshConsumer extends QuadRenderer implements Consumer<Mesh> {
    private final Maker editorQuad;
    
    protected MeshConsumer(
            BlockRenderInfo blockInfo, 
            ToIntFunction<BlockPos> brightnessFunc,
            BiFunction<RenderMaterialImpl.Value, QuadViewImpl, VertexCollector> collectorFunc, 
            AoCalculator aoCalc, 
            BooleanSupplier hasTransform,
            QuadTransform transform,
            Consumer<MutableQuadViewImpl> offsetFunc,
            Function<RenderMaterialImpl.Value, ShaderContext> contextFunction) {
        super(blockInfo, brightnessFunc, collectorFunc, aoCalc, hasTransform, transform, offsetFunc, contextFunction);
        editorQuad = new Maker();
    }

    /**
     * Where we handle all pre-buffer coloring, lighting, transformation, etc.
     * Reused for all mesh quads. Fixed baking array sized to hold largest possible
     * mesh quad.
     */
    private class Maker extends MutableQuadViewImpl implements QuadEmitter {
        {
            data = new int[MeshEncodingHelper.MAX_STRIDE];
            material(Canvas.INSTANCE.materialFinder().spriteDepth(RenderMaterialImpl.MAX_SPRITE_DEPTH).find());
        }

        // only used via RenderContext.getEmitter()
        @Override
        public Maker emit() {
            renderQuad(editorQuad);
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
        final Maker q = this.editorQuad;
        while (index < limit) {
            RenderMaterialImpl.Value mat = RenderMaterialImpl.byIndex(data[index]);
            final int stride = MeshEncodingHelper.stride(mat.spriteDepth());
            System.arraycopy(data, index, q.data(), 0, stride);
            q.load();
            index += stride;
            renderQuad(q);
        }
    }

    public QuadEmitter getEmitter() {
        editorQuad.clear();
        return editorQuad;
    }
}
