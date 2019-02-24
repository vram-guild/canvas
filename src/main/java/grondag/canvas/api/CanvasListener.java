package grondag.canvas.api;

/**
 * Implements and register
 */
public interface CanvasListener {
    /**
     * Will only be called when the status changes, so you may reliably infer the
     * previous status is the opposite of the new status.
     */
    public default void onStatusChange(boolean newEnabledStatus) {
    };

    /**
     * Called when rendered chunks, shaders, etc. are reloaded, due to a
     * configuration change, resource pack change, or user pressing F3 + A;
     */
    public default void onRenderReload() {
    };
}
