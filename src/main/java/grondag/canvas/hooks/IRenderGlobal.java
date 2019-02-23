package grondag.acuity.hooks;

import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;

public interface IRenderGlobal
{

    void setupTerrainFast(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator);

}
