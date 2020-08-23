package grondag.canvas;

import grondag.jmx.api.JmxInitializer;

public class CanvasJmxInit implements JmxInitializer {
	@Override
	public void onInitalizeJmx() {
		grondag.jmx.Configurator.loadVanillaModels = Configurator.forceJmxModelLoading || grondag.jmx.Configurator.loadVanillaModels;
	}
}
