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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

import grondag.canvas.RenderMaterialImpl.Value;
import grondag.canvas.buffering.BufferManager;
import grondag.canvas.core.PipelineManager;
import grondag.canvas.core.PipelineShaderManager;
import grondag.canvas.mesh.MeshBuilderImpl;
import grondag.frex.api.ExtendedRenderer;
import grondag.frex.api.RenderListener;
import grondag.frex.api.ShaderManager;
import net.fabricmc.fabric.api.client.model.fabric.MaterialFinder;
import net.fabricmc.fabric.api.client.model.fabric.MeshBuilder;
import net.fabricmc.fabric.api.client.model.fabric.RenderMaterial;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;

public class RendererImpl implements ExtendedRenderer {
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
    
    private final HashMap<Identifier, RenderMaterial> materialMap = new HashMap<>();

    private final ArrayList<WeakReference<RenderListener>> listeners = new ArrayList<WeakReference<RenderListener>>();

    private RendererImpl() {
    };

    @Override
    public MeshBuilder meshBuilder() {
        return new MeshBuilderImpl();
    }

    @Override
    public MaterialFinder materialFinder() {
        return new RenderMaterialImpl.Finder();
    }

    @Override
    public RenderMaterial materialById(Identifier id) {
        return materialMap.get(id);
    }

    @Override
    public boolean registerMaterial(Identifier id, RenderMaterial material) {
        if (materialMap.containsKey(id))
            return false;
        // cast to prevent acceptance of impostor implementations
        materialMap.put(id, material);
        return true;
    }

    @Override
    public ShaderManager shaderManager() {
        // TODO Auto-generated method stub
        return null;
    }

    public void forceReload() {
        Canvas.INSTANCE.getLog().info(I18n.translate("misc.info_reloading"));
        PipelineShaderManager.INSTANCE.forceReload();
        PipelineManager.INSTANCE.forceReload();
        BufferManager.forceReload();
        forEachListener(c -> c.onRenderReload());
    }

    public void forEachListener(Consumer<RenderListener> c) {
        Iterator<WeakReference<RenderListener>> it = this.listeners.iterator();
        while (it.hasNext()) {
            WeakReference<RenderListener> ref = it.next();
            RenderListener listener = ref.get();
            if (listener == null)
                it.remove();
            else
                c.accept(listener);
        }
    }

    @Override
    public boolean isEnabled() {
        return Canvas.isModEnabled();
    }

    @Override
    public void registerListener(RenderListener listener) {
        this.listeners.add(new WeakReference<RenderListener>(listener));
    }
}
