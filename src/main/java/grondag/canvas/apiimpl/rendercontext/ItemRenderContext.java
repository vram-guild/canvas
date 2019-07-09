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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.MeshImpl;
import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.buffer.packing.CanvasBufferBuilder;
import grondag.canvas.buffer.packing.VertexCollector;
import grondag.canvas.draw.TessellatorExt;
import grondag.canvas.material.ShaderContext;
import grondag.canvas.material.ShaderProps;
import grondag.canvas.material.VertexEncodingContext;
import grondag.canvas.varia.BakedQuadExt;
import grondag.frex.api.model.DynamicBakedModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

/**
 * Context for non-terrain block rendering.
 */
public class ItemRenderContext extends AbstractRenderContext implements RenderContext {
    private static int playerLightmap;
    
    public static void playerLightmap(int lightmap) {
        playerLightmap = lightmap;
    }
    
    public static int playerLightmap() {
        return playerLightmap;
    }
    
    private final ItemColors colorMap;
    private final Random random = new Random();
    private Tessellator tessellator = Tessellator.getInstance();
    private TessellatorExt tessellatorExt = (TessellatorExt)tessellator;
    private CanvasBufferBuilder canvasBuilder = (CanvasBufferBuilder) tessellator.getBufferBuilder();
    private int color;
    private ItemStack itemStack;
    private boolean smoothShading = false;
    private boolean enchantment = false;
    private final int[] quadData = new int[MeshEncodingHelper.MAX_STRIDE];
    private final RenderMaterialImpl.Value glintMaterial = Canvas.INSTANCE.materialFinder()
            .blendMode(0, BlockRenderLayer.TRANSLUCENT)
            .disableAo(0, true)
            .disableDiffuse(0, true)
            .emissive(0, true).find();
    
    private final Supplier<Random> randomSupplier = () -> {
        Random result = random;
        result.setSeed(42L);
        return random;
    };
    
    /**
     * When rendering an enchanted item, input stack will be empty. This value is
     * populated earlier in the call tree when this is the case so that we can
     * render correct geometry and only a single texture.
     */
    public ItemStack enchantmentStack;
    
    public ItemRenderContext(ItemColors colorMap) {
        this.colorMap = colorMap;
    }
    
    public void renderModel(DynamicBakedModel model, int color, ItemStack stack) {
        this.color = color;

        if (stack.isEmpty() && enchantmentStack != null) {
            enchantment = true;
            this.itemStack = enchantmentStack;
            enchantmentStack = null;
        } else {
            enchantment = false;
            this.itemStack = stack;
        }
        
        ((DynamicBakedModel) model).emitItemQuads(stack, randomSupplier, this);
        tessellatorExt.canvas_draw();
    }

    private class Maker extends MutableQuadViewImpl implements QuadEmitter {
        {
            data = quadData;
            clear();
        }

        @Override
        public Maker emit() {
            renderQuad();
            clear();
            return this;
        }
    }
    
    private final Maker editorQuad = new Maker();

    private final Consumer<Mesh> meshConsumer = (mesh) -> {
        MeshImpl m = (MeshImpl) mesh;
        final int[] data = m.data();
        final int limit = data.length;
        int index = 0;
        while (index < limit) {
            RenderMaterialImpl.Value mat = RenderMaterialImpl.byIndex(data[index]);
            final int stride = MeshEncodingHelper.stride(mat.spriteDepth());
            System.arraycopy(data, index, this.quadData, 0, stride);
            editorQuad.load();
            index += stride;
            renderQuad();
        }
    };
    
    /**
     * Vanilla normally renders items with flat shading - meaning only the last
     * vertex normal is applied for lighting purposes. We support non-cube vertex
     * normals so we need to change this to smooth for models that use them. We
     * don't change it unless needed because OpenGL state changes always impose a
     * performance cost and this happens for every item, every frame.
     */
    private void handleShading() {
        if (!smoothShading && editorQuad.hasVertexNormals()) {
            smoothShading = true;
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        }
    }

    private int quadColor() {
        final int colorIndex = editorQuad.colorIndex();
        int quadColor = color;
        if (!enchantment && quadColor == -1 && colorIndex != -1) {
            quadColor = colorMap.getColorMultiplier(itemStack, colorIndex);
            quadColor |= -16777216;
        }
        return quadColor;
    }

    private final VertexEncodingContext encodingContext = new VertexEncodingContext();
    
    private void renderQuad() {
        final MutableQuadViewImpl quad = editorQuad;
        if (!transform(editorQuad)) {
            return;
        }

        final ShaderContext context = tessellatorExt.canvas_context();
        final RenderMaterialImpl.Value mat = enchantment ? glintMaterial : quad.material();
        final int props = ShaderProps.classify(mat, quad, context);
        final VertexCollector output = canvasBuilder.vcList.get(mat, props);

        handleShading();
        
        ColorHelper.colorizeQuad(quad, quadColor());
        
        output.materialState().materialVertexFormat().encode(quad, encodingContext.prepare(mat, context, null, null, mat.shaderFlags()), output);
    }

    @Override
    public Consumer<Mesh> meshConsumer() {
        return meshConsumer;
    }

    private static final BlockState NULL_BLOCK_STATE = (BlockState) null;
    
    private final Consumer<BakedModel> fallbackConsumer = model -> {
        final boolean disableGuiLighting = !model.hasDepthInGui();
        for (int i = 0; i < 7; i++) {
            random.setSeed(42L);
            Direction face = ModelHelper.faceFromIndex(i);
            List<BakedQuad> quads = model.getQuads(NULL_BLOCK_STATE, face, random);
            final int count = quads.size();
            for (int j = 0; j < count; j++) {
                BakedQuad q = quads.get(j);
                final Value defaultMaterial = disableGuiLighting || ((BakedQuadExt)q).canvas_disableDiffuse()
                        ?  FallbackConsumer.MATERIAL_FLAT
                        :  FallbackConsumer.MATERIAL_SHADED;
                renderQuad(q, face, defaultMaterial);
            }
        }
    };

    private void renderQuad(BakedQuad quad, Direction cullFace, Value defaultMaterial) {
        System.arraycopy(quad.getVertexData(), 0, quadData, MeshEncodingHelper.VERTEX_START_OFFSET, 28);
        editorQuad.cullFace(cullFace);
        final int lightFace = ModelHelper.toFaceIndex(quad.getFace());
        editorQuad.lightFace(lightFace);
        editorQuad.nominalFace(lightFace);
        editorQuad.colorIndex(quad.getColorIndex());
        editorQuad.material(defaultMaterial);
        
        editorQuad.invalidateShape();
        editorQuad.geometryFlags();
            
        renderQuad();
    }
    
    @Override
    public Consumer<BakedModel> fallbackConsumer() {
        return fallbackConsumer;
    }

    @Override
    public QuadEmitter getEmitter() {
        editorQuad.clear();
        return editorQuad;
    }
}
