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

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import grondag.canvas.RenderMaterialImpl.Value;
import grondag.canvas.RendererImpl;
import grondag.canvas.aocalc.AoCalculator;
import grondag.canvas.helper.GeometryHelper;
import grondag.canvas.mesh.EncodingFormat;
import grondag.canvas.mesh.MutableQuadViewImpl;
import grondag.canvas.mixinext.BakedQuadExt;
import net.fabricmc.fabric.api.client.model.fabric.ModelHelper;
import net.fabricmc.fabric.api.client.model.fabric.QuadEmitter;
import net.fabricmc.fabric.api.client.model.fabric.RenderContext.QuadTransform;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;

public class TerrainFallbackConsumer extends AbstractQuadRenderer implements Consumer<BakedModel> {
    private static Value MATERIAL_FLAT = (Value) RendererImpl.INSTANCE.materialFinder().disableDiffuse(0, true).disableAo(0, true).find();
    private static Value MATERIAL_SHADED = (Value) RendererImpl.INSTANCE.materialFinder().disableAo(0, true).find();
    private static Value MATERIAL_AO_FLAT = (Value) RendererImpl.INSTANCE.materialFinder().disableDiffuse(0, true).find();
    private static Value MATERIAL_AO_SHADED = (Value) RendererImpl.INSTANCE.materialFinder().find();
    
    private final int[] editorBuffer = new int[28];
    private final ChunkRenderInfo chunkInfo;
    
    TerrainFallbackConsumer(BlockRenderInfo blockInfo, ChunkRenderInfo chunkInfo, AoCalculator aoCalc,
            QuadTransform transform) {
        super(blockInfo, chunkInfo::cachedBrightness, chunkInfo::getInitializedBuffer, aoCalc, transform);
        this.chunkInfo = chunkInfo;
        this.editorQuad = new Maker();
    }

    private class Maker extends MutableQuadViewImpl {
        {
            data = editorBuffer;
            material = MATERIAL_SHADED;
            baseIndex = -EncodingFormat.HEADER_STRIDE;
        }

        @Override
        public QuadEmitter emit() {
            // should not be called
            throw new UnsupportedOperationException("Fallback consumer does not support .emit()");
        }
    };

    @Override
    public void accept(BakedModel model) {
        final Supplier<Random> random = blockInfo.randomSupplier;
        final boolean useAo = blockInfo.defaultAo && model.useAmbientOcclusion();
        
        final BlockState blockState = blockInfo.blockState;
        for (int i = 0; i < 6; i++) {
            Direction face = ModelHelper.faceFromIndex(i);
            List<BakedQuad> quads = model.getQuads(blockState, face, random.get());
            final int count = quads.size();
            if (count != 0 && blockInfo.shouldDrawFace(face)) {
                for (int j = 0; j < count; j++) {
                    BakedQuad q = quads.get(j);
                    final Value defaultMaterial = ((BakedQuadExt)q).canvas_disableDiffuse()
                            ?  (useAo ? MATERIAL_AO_FLAT : MATERIAL_FLAT)
                            :  (useAo ? MATERIAL_AO_SHADED : MATERIAL_SHADED);
                    renderQuad(q, face, defaultMaterial);
                }
            }
        }

        List<BakedQuad> quads = model.getQuads(blockState, null, random.get());
        final int count = quads.size();
        if (count != 0) {
            for (int j = 0; j < count; j++) {
                BakedQuad q = quads.get(j);
                final Value defaultMaterial = ((BakedQuadExt)q).canvas_disableDiffuse()
                        ?  (useAo ? MATERIAL_AO_FLAT : MATERIAL_FLAT)
                        :  (useAo ? MATERIAL_AO_SHADED : MATERIAL_SHADED);
                renderQuad(q, null, defaultMaterial);
            }
        }
    }

    private void renderQuad(BakedQuad quad, Direction cullFace, Value defaultMaterial) {
        System.arraycopy(quad.getVertexData(), 0, editorBuffer, 0, 28);
        editorQuad.cullFace(cullFace);
        final Direction lightFace = quad.getFace();
        editorQuad.lightFace(lightFace);
        editorQuad.nominalFace(lightFace);
        editorQuad.colorIndex(quad.getColorIndex());
        editorQuad.material(defaultMaterial);
        
        if (editorQuad.material().hasAo) {
            editorQuad.invalidateShape();
        } else {
            // vanilla compatibility hack
            // For flat lighting, if cull face is set always use neighbor light.
            // Otherwise still need to ensure geometry is updated before offsets are applied
            if (cullFace == null) {
                editorQuad.invalidateShape();
                editorQuad.geometryFlags();
            } else {
                editorQuad.geometryFlags(GeometryHelper.AXIS_ALIGNED_FLAG | GeometryHelper.LIGHT_FACE_FLAG);
            }
        }
        
        super.renderQuad();
    }

    @Override
    protected void applyOffsets() {
        chunkInfo.applyOffsets(editorQuad);
    }
}
