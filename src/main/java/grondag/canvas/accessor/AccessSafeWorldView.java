package grondag.canvas.accessor;

import grondag.canvas.render.TerrainRenderContext;

/**
 * Used to stash block renderer reference in local scope during chunk rebuild,
 * thus avoiding repeated thread-local lookups.
 */
public interface AccessSafeWorldView {
    TerrainRenderContext fabric_getRenderer();

    void fabric_setRenderer(TerrainRenderContext renderer);
}
