package grondag.canvas;

import java.util.function.Supplier;

import grondag.frex.api.ShaderManager;
import grondag.frex.api.ShaderMaterial;
import net.fabricmc.fabric.api.client.model.fabric.RenderMaterial;

public class ShaderManagerImpl implements ShaderManager {
    public static final ShaderManagerImpl INSTANCE = new ShaderManagerImpl();
    
    @Override
    public ShaderMaterial shaderMaterial(RenderMaterial baseMaterial, Supplier<String> vertexSource, Supplier<String> fragmentSource, int flags) {
        // TODO Auto-generated method stub
        return null;
    }
}
