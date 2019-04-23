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

package grondag.canvas.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.apiimpl.rendercontext.legacy.ItemRenderContextOld;
import grondag.canvas.draw.TessellatorExt;
import grondag.canvas.material.ShaderContext;
import grondag.canvas.varia.GuiLightingHelper;
import grondag.frex.api.model.DynamicBakedModel;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.item.ItemColorMap;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.item.ItemStack;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Shadow
    protected abstract void renderQuads(BufferBuilder bufferBuilder, List<BakedQuad> quads, int color, ItemStack stack);

    @Shadow protected ItemColorMap colorMap;
    
    private ItemRenderContext context;
    private ItemRenderContextOld oldContext;
    
    private final TessellatorExt tessellatorExt = (TessellatorExt) Tessellator.getInstance();

    @Inject(method = "<init>*", at = @At("RETURN"), require = 1)
    private void afterInit(CallbackInfo ci) {
        context = new ItemRenderContext(colorMap);
        oldContext = new ItemRenderContextOld(colorMap);
    }
    
    /**
     * Save stack for enchantment glint renders - we won't otherwise have access to
     * it during the glint render because it receives an empty stack.
     */
    @Inject(at = @At("HEAD"), method = "renderItemAndGlow")
    private void hookRenderItemAndGlow(ItemStack stack, BakedModel model, CallbackInfo ci) {
        if(Configurator.enableItemRender) {
            if (!model.isBuiltin() && stack.hasEnchantmentGlint()) {
                context.enchantmentStack = stack;
            }
        } else {
            if (stack.hasEnchantmentGlint() && !((DynamicBakedModel) model).isVanillaAdapter()) {
                oldContext.enchantmentStack = stack;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "renderModel", cancellable = true)
    private void hookRenderModel(BakedModel model, int color, ItemStack stack, CallbackInfo ci) {
        DynamicBakedModel dynamicModel = (DynamicBakedModel) model;
        if(Configurator.enableItemRender) {
            // PERF: redirect most of the enables so we don't have to change state here each time
            GuiLightingHelper.suspend();
            context.renderModel(dynamicModel, color, stack);
            GuiLightingHelper.resume();
            ci.cancel();
        } else {
            if (!dynamicModel.isVanillaAdapter()) {
                oldContext.renderModel(dynamicModel, color, stack, this::renderQuads);
                ci.cancel();
            }
        }
    }
    
    @Inject(at = @At("HEAD"), method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/client/render/model/json/ModelTransformation$Type;Z)V")
    private void onRenderItem(ItemStack itemStack, BakedModel bakedModel, ModelTransformation.Type type, boolean flag, CallbackInfo ci) {
        if(Configurator.enableItemRender) {
            tessellatorExt.canvas_context(ShaderContext.ITEM_WORLD);
        }
    }
    
    @Inject(at = @At("HEAD"), method = "renderGuiItemModel")
    private void onRenderGuiItemModel(ItemStack itemStack, int int_1, int int_2, BakedModel bakedModel, CallbackInfo ci) {
        if(Configurator.enableItemRender) {
            tessellatorExt.canvas_context(ShaderContext.ITEM_GUI);
        }
    }
}
