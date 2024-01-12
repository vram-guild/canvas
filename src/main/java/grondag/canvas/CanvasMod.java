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

package grondag.canvas;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;

import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import io.vram.frex.api.config.FrexFeature;
import io.vram.frex.api.renderloop.RenderReloadListener;
import io.vram.frex.api.rendertype.RenderTypeExclusion;
import io.vram.frex.api.rendertype.VanillaShaderInfo;
import io.vram.frex.base.renderer.ao.AoFace;

import grondag.canvas.apiimpl.CanvasState;
import grondag.canvas.compat.Compat;
import grondag.canvas.config.ConfigManager;
import grondag.canvas.config.Configurator;

//FEAT: weather rendering
//FEAT: sky rendering
//FEAT: pbr textures
//PERF: disable animated textures when not in view
//PERF: improve light smoothing performance
//FEAT: weather uniforms
//FEAT: biome texture in shader

public class CanvasMod {
	public static final String MODID = "canvas";
	public static final Logger LOG = LogManager.getLogger("Canvas");
	public static final Style ERROR_STYLE = Style.EMPTY.withFont(Style.DEFAULT_FONT).withColor(0xFF6666);
	public static KeyMapping DEBUG_TOGGLE = new KeyMapping("key.canvas.debug_toggle", Character.valueOf('`'), "key.canvas.category");
	public static KeyMapping DEBUG_PREV = new KeyMapping("key.canvas.debug_prev", Character.valueOf('['), "key.canvas.category");
	public static KeyMapping DEBUG_NEXT = new KeyMapping("key.canvas.debug_next", Character.valueOf(']'), "key.canvas.category");
	public static KeyMapping RECOMPILE = new KeyMapping("key.canvas.recompile", Character.valueOf('='), "key.canvas.category");
	public static KeyMapping FLAWLESS_TOGGLE = new KeyMapping("key.canvas.flawless_toggle", -1, "key.canvas.category");
	public static KeyMapping PROFILER_TOGGLE = new KeyMapping("key.canvas.profiler_toggle", -1, "key.canvas.category");
	public static String versionString = "unknown";

	public static void init() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: Canvas mod initialization");
		}

		ConfigManager.init();

		FrexFeature.registerFeatures(
			FrexFeature.MATERIAL_SHADERS,
			FrexFeature.HELD_ITEM_LIGHTS,
			FrexFeature.VERTEX_TANGENT
		);

		if (Configurator.debugNativeMemoryAllocation.get()) {
			LOG.warn("Canvas is configured to enable native memory debug. This WILL cause slow performance and other issues.  Debug output will print at game exit.");
			Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
		}

		// entity shadows aren't worth
		RenderTypeExclusion.exclude(EntityRenderDispatcher.SHADOW_RENDER_TYPE);

		// FEAT: handle more of these with shaders
		RenderTypeExclusion.exclude(RenderType.armorGlint());
		RenderTypeExclusion.exclude(RenderType.armorEntityGlint());
		RenderTypeExclusion.exclude(RenderType.glint());
		RenderTypeExclusion.exclude(RenderType.glintDirect());
		RenderTypeExclusion.exclude(RenderType.glintTranslucent());
		RenderTypeExclusion.exclude(RenderType.entityGlint());
		RenderTypeExclusion.exclude(RenderType.entityGlintDirect());
		RenderTypeExclusion.exclude(RenderType.lines());
		RenderTypeExclusion.exclude(RenderType.lightning());

		// draw order is important and our sorting mechanism doesn't cover
		RenderTypeExclusion.exclude(RenderType.waterMask());
		RenderTypeExclusion.exclude(RenderType.endPortal());
		RenderTypeExclusion.exclude(RenderType.endGateway());

		ModelBakery.DESTROY_TYPES.forEach((renderLayer) -> {
			RenderTypeExclusion.exclude(renderLayer);
		});

		// currently we only handle quads
		RenderTypeExclusion.exclude(renderType -> {
			if (renderType.mode() != Mode.QUADS) {
				return true;
			}

			final var compositeState = ((CompositeRenderType) renderType).state;

			// Excludes glint, end portal, and other specialized render types that won't play nice with our current setup
			// Excludes render types with custom shaders
			// Excludes render types without a texture
			if (compositeState.texturingState != RenderStateShard.DEFAULT_TEXTURING
					|| !(compositeState.textureState instanceof TextureStateShard)
					|| VanillaShaderInfo.get(compositeState.shaderState) == VanillaShaderInfo.MISSING) {
				return true;
			}

			return false;
		});

		RenderReloadListener.register(CanvasState::reload);
		AoFace.clampExteriorVertices(Configurator.clampExteriorVertices);
		Compat.init();
	}

	public static void displayClientError(String errorMessage) {
		@SuppressWarnings("resource")
		final LocalPlayer localPlayer = Minecraft.getInstance().player;

		if (localPlayer != null) {
			final Component message = Component.literal("[Canvas] ").append(Component.literal(errorMessage).setStyle(ERROR_STYLE));
			localPlayer.displayClientMessage(message, false);
		}
	}
}
