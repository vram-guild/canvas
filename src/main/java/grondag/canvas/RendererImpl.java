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

package grondag.canvas;

import java.util.HashMap;
import java.util.function.BooleanSupplier;

import grondag.canvas.RenderMaterialImpl.Finder;
import grondag.canvas.RenderMaterialImpl.Value;
import grondag.canvas.buffering.VboBufferManager;
import grondag.canvas.core.PipelineManager;
import grondag.canvas.core.RenderPipelineImpl;
import grondag.canvas.mesh.MeshBuilderImpl;
import grondag.frex.api.extended.ExtendedRenderer;
import grondag.frex.api.extended.Pipeline;
import grondag.frex.api.extended.PipelineBuilder;
import grondag.frex.api.extended.RenderCondition;
import grondag.frex.api.extended.RenderReloadCallback;
import grondag.frex.api.core.MeshBuilder;
import grondag.frex.api.core.RenderMaterial;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;

public class RendererImpl implements ExtendedRenderer, RenderReloadCallback {
    public static final RendererImpl INSTANCE = new RendererImpl();

    public static final RenderMaterialImpl.Value MATERIAL_STANDARD = (Value) INSTANCE.materialFinder().find();
//    private static final Value[] VANILLA_MATERIALS = new Value[4];


    static {
        INSTANCE.registerMaterial(RenderMaterial.MATERIAL_STANDARD, MATERIAL_STANDARD);
        
//        Finder finder = new Finder();
//        for(BlockRenderLayer layer : RenderMaterialImpl.BLEND_MODES) {
//            VANILLA_MATERIALS[layer.ordinal()] = finder.clear().blendMode(0, layer).find();
//        }
    }

//    public static Value vanillaMaterial(BlockRenderLayer layer) {
//        return VANILLA_MATERIALS[layer.ordinal()];
//    }
    
    private final HashMap<Identifier, Value> materialMap = new HashMap<>();

    private final HashMap<Identifier, RenderPipelineImpl> pipelineMap = new HashMap<>();
    
    private final HashMap<Identifier, RenderConditionImpl> conditionMap = new HashMap<>();
    
    private RendererImpl() {
        RenderReloadCallback.EVENT.register(this);
    };

    @Override
    public MeshBuilder meshBuilder() {
        return new MeshBuilderImpl();
    }

    @Override
    public Finder materialFinder() {
        return new RenderMaterialImpl.Finder();
    }

    @Override
    public Value materialById(Identifier id) {
        return materialMap.get(id);
    }

    @Override
    public boolean registerMaterial(Identifier id, RenderMaterial material) {
        if (materialMap.containsKey(id))
            return false;
        // cast to prevent acceptance of impostor implementations
        materialMap.put(id, (Value) material);
        return true;
    }

    @Override
    public void reload() {
        Canvas.INSTANCE.getLog().info(I18n.translate("misc.info_reloading"));
        PipelineManager.INSTANCE.forceReload();
        VboBufferManager.forceReload();
    }

    @Override
    public PipelineBuilder pipelineBuilder() {
        return new PipelineBuilderImpl();
    }

    @Override
    public RenderPipelineImpl pipelineById(Identifier id) {
        return pipelineMap.get(id);
    }

    @Override
    public boolean registerPipeline(Identifier id, Pipeline pipeline) {
        if (pipelineMap.containsKey(id))
            return false;
        // cast to prevent acceptance of impostor implementations
        pipelineMap.put(id, (RenderPipelineImpl) pipeline);
        return true;
    }

    @Override
    public RenderCondition createCondition(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
        return new RenderConditionImpl(supplier, affectBlocks, affectItems);
    }
    
    @Override
    public boolean registerCondition(Identifier id, RenderCondition condition) {
        if (conditionMap.containsKey(id))
            return false;
        // cast to prevent acceptance of impostor implementations
        conditionMap.put(id, (RenderConditionImpl) condition);
        return true;
    }
    
    @Override
    public RenderCondition conditionById(Identifier id) {
        return conditionMap.get(id);
    }
}
