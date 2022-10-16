/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.config;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.CanvasState;
import grondag.canvas.pipeline.config.option.OptionConfig;

public class ConfigManager {
	static final ConfigData DEFAULTS = ConfigData.DEFAULT_VALUES;
	public static final Gson GSON = new GsonBuilder().create();
	public static final Jankson JANKSON = Jankson.builder().build();

	enum Reload {
		RELOAD_EVERYTHING,
		RELOAD_PIPELINE,
		DONT_RELOAD
	}

	static File configFile;
	static File pipelineFile;

	public static void init() {
		configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "canvas.json5");

		if (configFile.exists()) {
			ConfigManager.loadConfig();
		} else {
			ConfigManager.saveConfig();
		}

		pipelineFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "canvas_pipeline_options.json5");
	}

	public static Component[] parse(String key) {
		return Arrays.stream(I18n.get(key).split(";")).map(s -> Component.literal(s)).collect(Collectors.toList()).toArray(new Component[0]);
	}

	public static void initPipelineOptions(OptionConfig[] options) {
		if (options == null || options.length == 0) {
			return;
		}

		try {
			final JsonObject configJson = pipelineFile.exists() ? JANKSON.load(pipelineFile) : new JsonObject();

			for (final OptionConfig cfg : options) {
				cfg.readConfig(configJson);
				cfg.writeConfig(configJson);
			}

			try (FileOutputStream out = new FileOutputStream(pipelineFile, false)) {
				out.write(configJson.toJson(true, true).getBytes());
				out.flush();
				out.close();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			CanvasMod.LOG.error("Error loading pipeline config. Using default values.");
		}
	}

	public static void savePipelineOptions(OptionConfig[] options) {
		if (options == null || options.length == 0) {
			return;
		}

		try {
			final JsonObject configJson = JANKSON.load(pipelineFile);

			for (final OptionConfig cfg : options) {
				cfg.writeConfig(configJson);
			}

			if (!pipelineFile.exists()) {
				pipelineFile.createNewFile();
			}

			try (FileOutputStream out = new FileOutputStream(pipelineFile, false)) {
				out.write(configJson.toJson(true, true).getBytes());
				out.flush();
				out.close();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			CanvasMod.LOG.error("Error loading pipeline config. Using default values.");
		}

		CanvasState.recompileIfNeeded(true);
	}

	private static void saveConfig() {
		final ConfigData config = new ConfigData();

		Configurator.writeToConfig(config);

		try {
			final JsonObject json = (JsonObject) JANKSON.toJson(config);
			final String result = json.toJson(true, true);

			if (!configFile.exists()) {
				configFile.createNewFile();
			}

			try (FileOutputStream out = new FileOutputStream(configFile, false)) {
				out.write(result.getBytes());
				out.flush();
				out.close();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			CanvasMod.LOG.error("Unable to save config.");
			return;
		}
	}

	static void loadConfig() {
		ConfigData config = new ConfigData();

		try {
			final JsonObject configJson = JANKSON.load(configFile);
			final String regularized = configJson.toJson(false, false, 0);
			config = GSON.fromJson(regularized, ConfigData.class);
		} catch (final Exception e) {
			e.printStackTrace();
			CanvasMod.LOG.error("Unable to load config. Using default values.");
		}

		Configurator.readFromConfig(config, true);
	}

	static void saveUserInput(Reload reload) {
		saveConfig();

		switch (reload) {
			case RELOAD_EVERYTHING -> Minecraft.getInstance().levelRenderer.allChanged();
			case RELOAD_PIPELINE -> CanvasState.recompileIfNeeded(true);
			case DONT_RELOAD -> { }
		}
	}

	/**
	 * Legacy format compatibility.
	 */
	public static Component parseTooltip(String key) {
		String translated = I18n.get(key);
		translated = translated.replace(";", " ");
		translated = translated.replace("  ", " ");
		return Component.literal(translated);
	}
}
