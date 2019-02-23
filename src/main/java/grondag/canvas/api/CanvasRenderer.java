package grondag.canvas.api;

import net.fabricmc.fabric.api.client.model.fabric.Renderer;

public interface CanvasRenderer extends Renderer {
    /**
     * Access to {@link ShaderManager} for attachment of shaders to standard materials.
     * Will return null if this renderer does not support that feature.
     */
    ShaderManager shaderManager();
    
    /**
     * Will be false if user has disabled Canvas in configuration.
     */
    boolean isEnabled();
    
    /**
     * Use if you need callbacks for status changes.
     * Holds a weak reference, so no need to remove listeners that fall out of scope.
     */
    void registerListener(CanvasListener lister);
}
