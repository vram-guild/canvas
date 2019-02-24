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
import java.util.function.ToIntBiFunction;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.fabricmc.fabric.api.client.model.fabric.Mesh;
import net.fabricmc.fabric.api.client.model.fabric.QuadEmitter;
import net.fabricmc.fabric.api.client.model.fabric.RenderContext.QuadTransform;
import grondag.canvas.RenderMaterialImpl;
import grondag.canvas.RenderMaterialImpl.Value;
import grondag.canvas.RendererImpl;
import grondag.canvas.aocalc.AoCalculator;
import grondag.canvas.core.CompoundBufferBuilder;
import grondag.canvas.mesh.EncodingFormat;
import grondag.canvas.mesh.MeshImpl;
import grondag.canvas.mesh.MutableQuadViewImpl;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * Consumer for pre-baked meshes. Works by copying the mesh data to a "editor"
 * quad held in the instance, where all transformations are applied before
 * buffering.
 */
public abstract class AbstractMeshConsumer extends AbstractQuadRenderer implements Consumer<Mesh> {
    protected AbstractMeshConsumer(BlockRenderInfo blockInfo, ToIntBiFunction<BlockState, BlockPos> brightnessFunc,
            Int2ObjectFunction<CompoundBufferBuilder> bufferFunc, AoCalculator aoCalc, QuadTransform transform) {
        super(blockInfo, brightnessFunc, bufferFunc, aoCalc, transform);
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
                renderQuad(this);
            }
            clear();
            return this;
        }
    };

    private final Maker editorQuad = new Maker();

    protected int[] lightmaps = new int[4];

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
            renderQuad(editorQuad);
        }
    }

    public QuadEmitter getEmitter() {
        editorQuad.clear();
        return editorQuad;
    }

    private void renderQuad(MutableQuadViewImpl q) {
        if (!transform.transform(editorQuad)) {
            return;
        }

        final RenderMaterialImpl.Value mat = q.material();

        if (mat.hasEmissive) {
            captureLightmaps(q);
        }

        if (mat.hasAo && MinecraftClient.isAmbientOcclusionEnabled()) {
            // needs to happen before offsets are applied
            aoCalc.compute(q, false);
        }

        applyOffsets(q);

        tesselateQuad(q, mat, 0);

        final int textureCount = mat.spriteDepth();
        for (int t = 1; t < textureCount; t++) {
            for (int i = 0; i < 4; i++) {
                q.spriteColor(i, 0, q.spriteColor(i, t));
                q.sprite(i, 0, q.spriteU(i, t), q.spriteV(i, t));
            }
            tesselateQuad(q, mat, t);
        }
    }

    private void captureLightmaps(MutableQuadViewImpl q) {
        final int[] data = q.data();
        final int[] lightmaps = this.lightmaps;
        lightmaps[0] = data[EncodingFormat.VERTEX_START_OFFSET + 6];
        lightmaps[1] = data[EncodingFormat.VERTEX_START_OFFSET + 6 + 7];
        lightmaps[2] = data[EncodingFormat.VERTEX_START_OFFSET + 6 + 14];
        lightmaps[3] = data[EncodingFormat.VERTEX_START_OFFSET + 6 + 21];
    }

    protected abstract void applyOffsets(MutableQuadViewImpl quad);

    /**
     * Determines color index and render layer, then routes to appropriate tesselate
     * routine based on material properties.
     */
    private void tesselateQuad(MutableQuadViewImpl quad, RenderMaterialImpl.Value mat, int textureIndex) {
        final int colorIndex = mat.disableColorIndex(textureIndex) ? -1 : quad.colorIndex();
        final int renderLayer = blockInfo.layerIndexOrDefault(mat.blendMode(textureIndex));

        if (blockInfo.defaultAo && !mat.disableAo(textureIndex)) {
            if (mat.emissive(textureIndex)) {
                tesselateSmoothEmissive(quad, renderLayer, colorIndex, lightmaps);
            } else {
                tesselateSmooth(quad, renderLayer, colorIndex);
            }
        } else {
            if (mat.emissive(textureIndex)) {
                tesselateFlatEmissive(quad, renderLayer, colorIndex, lightmaps);
            } else {
                tesselateFlat(quad, renderLayer, colorIndex);
            }
        }
    }
}