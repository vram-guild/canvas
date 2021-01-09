/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.pipeline.config.option.OptionConfig;

public class ConfigManager {
	static final ConfigData DEFAULTS = new ConfigData();
	public static final Gson GSON = new GsonBuilder().create();
	public static final Jankson JANKSON = Jankson.builder().build();

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

	public static Text[] parse(String key) {
		return Arrays.stream(I18n.translate(key).split(";")).map(s -> new LiteralText(s)).collect(Collectors.toList()).toArray(new Text[0]);
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

		Canvas.INSTANCE.recompile();
	}

	static void saveConfig() {
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

		Configurator.readFromConfig(config);
	}

	@SuppressWarnings("resource")
	static void saveUserInput() {
		saveConfig();

		if (Configurator.reload) {
			MinecraftClient.getInstance().worldRenderer.reload();
		}
	}
}
