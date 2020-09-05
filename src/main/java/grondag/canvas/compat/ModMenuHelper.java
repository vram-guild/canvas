package grondag.canvas.compat;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;

public class ModMenuHelper implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return Configurator::display;
	}

	@Override
	public String getModId() {
		return CanvasMod.MODID;
	}
}
