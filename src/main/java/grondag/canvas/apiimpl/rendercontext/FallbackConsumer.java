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

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.buffer.packing.VertexCollector;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.ShaderContext;
import grondag.canvas.varia.BakedQuadExt;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FallbackConsumer extends QuadRenderer implements Consumer<BakedModel> {
    protected static Value MATERIAL_FLAT = (Value) Canvas.INSTANCE.materialFinder().disableDiffuse(0, true).disableAo(0, true).find();
    protected static Value MATERIAL_SHADED = (Value) Canvas.INSTANCE.materialFinder().disableAo(0, true).find();
    protected static Value MATERIAL_AO_FLAT = (Value) Canvas.INSTANCE.materialFinder().disableDiffuse(0, true).find();
    protected static Value MATERIAL_AO_SHADED = (Value) Canvas.INSTANCE.materialFinder().find();
    
    protected final int[] editorBuffer = new int[MeshEncodingHelper.HEADER_STRIDE + MeshEncodingHelper.VANILLA_STRIDE];

    private final Maker editorQuad;
    
    protected class Maker extends MutableQuadViewImpl {
        {
            data = editorBuffer;
            material = MATERIAL_SHADED;
        }

        @Override
        public QuadEmitter emit() {
            // should not be called
            throw new UnsupportedOperationException("Fallback consumer does not support .emit()");
        }
    };
    
    FallbackConsumer(
            BlockRenderInfo blockInfo, 
            ToIntFunction<BlockPos> brightnessFunc, 
            BiFunction<RenderMaterialImpl.Value, QuadViewImpl, VertexCollector> collectorFunc, 
            AoCalculator aoCalc,
            BooleanSupplier hasTransform,
            QuadTransform transform, 
            Consumer<MutableQuadViewImpl> offsetFunc,
            Function<RenderMaterialImpl.Value, ShaderContext> contextFunc) {
        super(blockInfo, brightnessFunc, collectorFunc, aoCalc, hasTransform, transform, offsetFunc, contextFunc);
        this.editorQuad = new Maker();
    }
    
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
                    renderQuad(q, i, defaultMaterial);
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
                renderQuad(q, ModelHelper.NULL_FACE_ID, defaultMaterial);
            }
        }
    }
    
    private void renderQuad(BakedQuad quad, int cullFace, Value defaultMaterial) {
        final Maker editorQuad = this.editorQuad;
        System.arraycopy(quad.getVertexData(), 0, editorBuffer, MeshEncodingHelper.HEADER_STRIDE, 28);
        editorQuad.cullFace(cullFace);
        final int lightFace = ModelHelper.toFaceIndex(quad.getFace());
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
            if (cullFace == ModelHelper.NULL_FACE_ID) {
                editorQuad.invalidateShape();
                editorQuad.geometryFlags();
            } else {
                editorQuad.geometryFlags(GeometryHelper.AXIS_ALIGNED_FLAG | GeometryHelper.LIGHT_FACE_FLAG);
            }
        }
        
        if(Configurator.preventDepthFighting) {
            preventDepthFighting();
        }
        
        super.renderQuad(editorQuad);
    }
    
    private static final float MIN_Z_LOW = 0.002f;
    private static final float MIN_Z_HIGH = 1 - MIN_Z_LOW;
    
    @SuppressWarnings("unchecked")
    private static final Consumer<Maker> DEPTH_FIGHTERS[] = new Consumer[6];
    
    static {
        DEPTH_FIGHTERS[Direction.DOWN.ordinal()] = q -> {
            for(int i = 0; i < 4; i++) {
                if(q.y(i) > MIN_Z_HIGH) {
                    q.y(i,MIN_Z_HIGH);
                }
            }
        };
        
        DEPTH_FIGHTERS[Direction.UP.ordinal()] = q -> {
            for(int i = 0; i < 4; i++) {
                if(q.y(i) < MIN_Z_LOW) {
                    q.y(i, MIN_Z_LOW);
                }
            }
        };
        
        DEPTH_FIGHTERS[Direction.NORTH.ordinal()] = q -> {
            for(int i = 0; i < 4; i++) {
                if(q.z(i) > MIN_Z_HIGH) {
                    q.z(i, MIN_Z_HIGH);
                }
            }
        };
        
        DEPTH_FIGHTERS[Direction.SOUTH.ordinal()] = q -> {
            for(int i = 0; i < 4; i++) {
                if(q.z(i) < MIN_Z_LOW) {
                    q.z(i, MIN_Z_LOW);
                }
            }
        };
        
        DEPTH_FIGHTERS[Direction.EAST.ordinal()] = q -> {
            for(int i = 0; i < 4; i++) {
                if(q.x(i) < MIN_Z_LOW) {
                    q.x(i, MIN_Z_LOW);
                }
            }
        };
        
        DEPTH_FIGHTERS[Direction.WEST.ordinal()] = q -> {
            for(int i = 0; i < 4; i++) {
                if(q.x(i) > MIN_Z_HIGH) {
                    q.x(i, MIN_Z_HIGH);
                }
            }
        };
    }
    
    private void preventDepthFighting() {
        if(editorQuad.cullFaceId() == ModelHelper.NULL_FACE_ID) {
            Maker q = editorQuad;
            DEPTH_FIGHTERS[q.lightFaceId()].accept(q);
        }
    }
}
