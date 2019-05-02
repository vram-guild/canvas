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

import static grondag.canvas.apiimpl.util.GeometryHelper.LIGHT_FACE_FLAG;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.util.AoCalculator;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.buffer.packing.VertexCollector;
import grondag.canvas.material.ShaderContext;
import grondag.canvas.material.VertexEncoder;
import grondag.frex.api.render.RenderContext.QuadTransform;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * Base quad-rendering class for fallback and mesh consumers. Has most of the
 * actual buffer-time lighting and coloring logic.
 */
public class QuadRenderer {
    public static final Consumer<MutableQuadViewImpl> NO_OFFSET = (q) -> {};
    
    protected final ToIntBiFunction<BlockState, BlockPos> brightnessFunc;
    protected final BiFunction<RenderMaterialImpl.Value, QuadViewImpl, VertexCollector> collectorFunc;
    protected final BlockRenderInfo blockInfo;
    protected final AoCalculator aoCalc;
    protected final QuadTransform transform;
    protected MutableQuadViewImpl editorQuad;
    protected final Consumer<MutableQuadViewImpl> offsetFunc;
    protected final Function<RenderMaterialImpl.Value, ShaderContext> contextFunc;
    QuadRenderer(
            BlockRenderInfo blockInfo, 
            ToIntBiFunction<BlockState, BlockPos> brightnessFunc,
            BiFunction<RenderMaterialImpl.Value, QuadViewImpl, VertexCollector> collectorFunc, 
            AoCalculator aoCalc, 
            QuadTransform transform,
            Consumer<MutableQuadViewImpl> offsetFunc,
            Function<RenderMaterialImpl.Value, ShaderContext> contextFunc) {
        this.blockInfo = blockInfo;
        this.brightnessFunc = brightnessFunc;
        this.collectorFunc = collectorFunc;
        this.aoCalc = aoCalc;
        this.transform = transform;
        this.offsetFunc = offsetFunc;
        this.contextFunc = contextFunc;
    }

    /** handles block color and red-blue swizzle, common to all renders */
    private void colorizeQuad(MutableQuadViewImpl q) {
        final int blockColorIndex = q.colorIndex();
        ColorHelper.colorizeQuad(q, blockColorIndex == -1 ? -1 : (blockInfo.blockColor(blockColorIndex) | 0xFF000000));
    }

    /** final output step, common to all renders */
    protected final void renderQuad() {
        final MutableQuadViewImpl q = editorQuad;
        
        if (!transform.transform(q)) {
            return;
        }
        
        if (!blockInfo.shouldDrawFace(q.cullFace())) {
            return;
        }

        final RenderMaterialImpl.Value mat = q.material().forRenderLayer(blockInfo.defaultLayerIndex);
        final VertexCollector output = collectorFunc.apply(mat, q);
        
        final boolean isAo = blockInfo.defaultAo && mat.hasAo;
        if (isAo) {
            // needs to happen before offsets are applied
            aoCalc.compute(q);
        }

        offsetFunc.accept(q);
        
        colorizeQuad(q);
        
        if(isAo) {
            lightSmooth(q);
        } else {
            lightFlat(q);
        }
        
        if(Configurator.preventTerrainShadingAnisotropy && (q.geometryFlags() & GeometryHelper.CUBIC_FLAG) == GeometryHelper.CUBIC_FLAG) {
            encodeSmoothQuad(q, output, mat, isAo);
        } else {
            encodeQuad(q, output, mat, isAo);
        }
    }
    
    private void encodeQuad(MutableQuadViewImpl q, VertexCollector output, RenderMaterialImpl.Value mat, boolean isAo) {
        VertexEncoder.encodeBlock(q, mat, contextFunc.apply(mat), output, blockInfo.blockPos, isAo ? aoCalc.ao : null);
    }
    
    //UGLY: ugly hack is ugly
    private final int[] smoothData = new int[MeshEncodingHelper.MAX_STRIDE];
    
    private void encodeSmoothQuad(MutableQuadViewImpl q, VertexCollector output, RenderMaterialImpl.Value mat, boolean isAo) {
        final int depth = mat.spriteDepth();
        
        final float sx = (q.x(0) + q.x(1) + q.x(2) + q.x(3)) * 0.25f;
        final float sy = (q.y(0) + q.y(1) + q.y(2) + q.y(3)) * 0.25f;
        final float sz = (q.z(0) + q.z(1) + q.z(2) + q.z(3)) * 0.25f;
        final float su0 = (q.spriteU(0, 0) + q.spriteU(1, 0) + q.spriteU(2, 0) + q.spriteU(3, 0)) * 0.25f;
        final float sv0 = (q.spriteV(0, 0) + q.spriteV(1, 0) + q.spriteV(2, 0) + q.spriteV(3, 0)) * 0.25f;
        final int sc0 = centerColor(q, 0);
        
        float su1 = 0, sv1 = 0, su2 = 0, sv2 = 0;
        int sc1 = 0, sc2 = 0;
        
        if(depth > 1) {
            su1 = (q.spriteU(0, 1) + q.spriteU(1, 1) + q.spriteU(2, 1) + q.spriteU(3, 1)) * 0.25f;
            sv1 = (q.spriteV(0, 1) + q.spriteV(1, 1) + q.spriteV(2, 1) + q.spriteV(3, 1)) * 0.25f;
            sc1 = centerColor(q, 1);
            
            if(depth == 3) {
                su2 = (q.spriteU(0, 2) + q.spriteU(1, 2) + q.spriteU(2, 2) + q.spriteU(3, 2)) * 0.25f;
                sv2 = (q.spriteV(0, 2) + q.spriteV(1, 2) + q.spriteV(2, 2) + q.spriteV(3, 2)) * 0.25f;
                sc2 = centerColor(q, 2);
            }
        }
        
        final int sl = centerLight(q);
        final float sao = isAo ? 0.25f * (aoCalc.ao[0] + aoCalc.ao[1] + aoCalc.ao[2] + aoCalc.ao[3]) : 1;
        
        final int[] data = q.data();
        final int len = data.length;
        
        System.arraycopy(data, 0, smoothData, 0, len);
        
        float aoSwap = aoCalc.ao[3];
        aoCalc.ao[3] = sao;
        
        q.pos(3, sx, sy, sz);
        q.sprite(3, 0, su0, sv0);
        q.spriteColor(3, 0, sc0);
        if(q.hasVertexNormals()) {
            calcCenterNormal(q);
            q.normal(3, centerNormal);
        }
        q.lightmap(3, sl);
        
        if(depth > 1) {
            q.sprite(3, 1, su1, sv1);
            q.spriteColor(3, 1, sc1);
            
            if(depth == 3) {
                q.sprite(3, 2, su2, sv2);
                q.spriteColor(3, 2, sc2);
            }
        }
        
        this.encodeQuad(q, output, mat, isAo);
        
        System.arraycopy(smoothData, 0, data, 0, len);
        
        // FIXME: need to restore normal state - load breaks with fallback context
//        q.load();
        aoCalc.ao[3] = aoSwap;
        aoCalc.ao[1] = sao;
        
        q.pos(1, sx, sy, sz);
        q.sprite(1, 0, su0, sv0);
        q.spriteColor(1, 0, sc0);
        if(q.hasVertexNormals()) {
            calcCenterNormal(q);
            q.normal(1, centerNormal);
        }
        q.lightmap(1, sl);
        
        if(depth > 1) {
            q.sprite(1, 1, su1, sv1);
            q.spriteColor(1, 1, sc1);
            
            if(depth == 3) {
                q.sprite(1, 2, su2, sv2);
                q.spriteColor(1, 2, sc2);
            }
        }
        
        this.encodeQuad(q, output, mat, isAo);
    }
    
    private int centerLight(MutableQuadViewImpl q) {
        int l0 = q.lightmap(0);
        int l1 = q.lightmap(1);
        int l2 = q.lightmap(2);
        int l3 = q.lightmap(3);
        
        if(l0 == l1 && l0 == l2 && l0 == l3) {
            return l0;
        }
        
        int blockLight = Math.round(0.25f * ((l0 & 0xFF) + (l1 & 0xFF) + (l2 & 0xFF) + (l3 & 0xFF)));
        int skyLight = Math.round(0.25f * (((l0 >> 16) & 0xFF) + ((l1 >> 16) & 0xFF) + ((l2 >> 16) & 0xFF) + ((l3 >> 16) & 0xFF)));
        return blockLight | (skyLight << 16);
    }
    
    private final net.minecraft.client.util.math.Vector3f centerNormal = new net.minecraft.client.util.math.Vector3f();
    private void calcCenterNormal(MutableQuadViewImpl q) {
        final net.minecraft.client.util.math.Vector3f fn = q.faceNormal();
        
        float xn = 0;
        float yn = 0;
        float zn = 0;
        
        for(int j = 0; j < 4; j++) {
            if(q.hasNormal(j)) {
                xn += q.normalX(j);
                yn += q.normalY(j);
                zn += q.normalZ(j);
            } else {
                xn += fn.x();
                yn += fn.y();
                zn += fn.z();
            }
        }
        
        // renormalize
        final float scale = 1f / (float) Math.sqrt(xn * xn + yn * yn + zn * zn);
        centerNormal.set(xn * scale, yn * scale, zn * scale);
    }
    
    private int centerColor(MutableQuadViewImpl q, int spriteIndex) {
        int c0 = q.spriteColor(0, spriteIndex);
        int c1 = q.spriteColor(1, spriteIndex);
        int c2 = q.spriteColor(2, spriteIndex);
        int c3 = q.spriteColor(3, spriteIndex);
        if(c0 == c1 && c0 == c2 && c0 == c3) {
            return c0;
        }
        int a = grondag.fermion.color.ColorHelper.interpolate(c0, c1, 0.5f);
        int b = grondag.fermion.color.ColorHelper.interpolate(c2, c3, 0.5f);
        return grondag.fermion.color.ColorHelper.interpolate(a, b, 0.5f);
    }
    
    /** for non-emissive mesh quads and all fallback quads with smooth lighting */
    private void lightSmooth(MutableQuadViewImpl q) {
        for (int i = 0; i < 4; i++) {
            q.lightmap(i, ColorHelper.maxBrightness(q.lightmap(i), aoCalc.light[i]));
        }
    }

    /** for non-emissive mesh quads and all fallback quads with flat lighting */
    private void lightFlat(MutableQuadViewImpl quad) {
        final int brightness = flatBrightness(quad, blockInfo.blockState, blockInfo.blockPos);
        for (int i = 0; i < 4; i++) {
            quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), brightness));
        }
    }

    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    /**
     * Handles geometry-based check for using self brightness or neighbor
     * brightness. That logic only applies in flat lighting.
     */
    private int flatBrightness(MutableQuadViewImpl quad, BlockState blockState, BlockPos pos) {
        mpos.set(pos);
        if ((quad.geometryFlags() & LIGHT_FACE_FLAG) != 0) {
            mpos.setOffset(quad.lightFace());
        }
        return brightnessFunc.applyAsInt(blockState, mpos);
    }
}
