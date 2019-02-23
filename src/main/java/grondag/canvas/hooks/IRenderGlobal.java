package grondag.canvas.hooks;

import net.minecraft.entity.Entity;

public interface IRenderGlobal
{
    void setupTerrainFast(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator);
}
