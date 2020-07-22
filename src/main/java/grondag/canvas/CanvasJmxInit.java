package grondag.canvas;

import net.fabricmc.loader.api.FabricLoader;

import grondag.jmx.api.JmxInitializer;

public class CanvasJmxInit implements JmxInitializer {
	@Override
	public void onInitalizeJmx() {
		grondag.jmx.Configurator.loadVanillaModels = (Configurator.forceJmxModelLoading || grondag.jmx.Configurator.loadVanillaModels)
				// CBT refuses to transform non-vanilla models
				&& !FabricLoader.getInstance().isModLoaded("connected_block_textures");
	}
}
