/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package grondag.canvas.material.state;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.mixin.AccessMultiPhaseParameters;
import grondag.canvas.mixin.AccessTexture;
import grondag.canvas.mixinterface.EntityRenderDispatcherExt;
import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.canvas.mixinterface.RenderLayerExt;
import grondag.frex.api.material.MaterialFinder;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

// segregates render layer references from mod init
public final class RenderLayerHelper {
	private RenderLayerHelper() {}

	private static final ReferenceOpenHashSet<RenderLayer> EXCLUSIONS = new ReferenceOpenHashSet<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	static {
		// entity shadows aren't worth
		EXCLUSIONS.add(((EntityRenderDispatcherExt) MinecraftClient.getInstance().getEntityRenderDispatcher()).canvas_shadowLayer());

		// FEAT: handle more of these with shaders
		EXCLUSIONS.add(RenderLayer.getArmorGlint());
		EXCLUSIONS.add(RenderLayer.getArmorEntityGlint());
		EXCLUSIONS.add(RenderLayer.getGlint());
		EXCLUSIONS.add(RenderLayer.getDirectGlint());
		EXCLUSIONS.add(RenderLayer.method_30676());
		EXCLUSIONS.add(RenderLayer.getEntityGlint());
		EXCLUSIONS.add(RenderLayer.getDirectEntityGlint());
		EXCLUSIONS.add(RenderLayer.getLines());
		EXCLUSIONS.add(RenderLayer.getLightning());
		// draw order is important and our sorting mechanism doesn't cover
		EXCLUSIONS.add(RenderLayer.getWaterMask());
		EXCLUSIONS.add(RenderLayer.getEndPortal(0));

		ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((renderLayer) -> {
			EXCLUSIONS.add(renderLayer);
		});
	}

	public static boolean isExcluded(RenderLayer layer) {
		return EXCLUSIONS.contains(layer);
	}

	public static BlendMode blendModeFromLayer(RenderLayer layer) {
		final AccessMultiPhaseParameters params = ((MultiPhaseExt) layer).canvas_phases();

		if (params.getTransparency() == RenderPhase.TRANSLUCENT_TRANSPARENCY) {
			return BlendMode.TRANSLUCENT;
		} else if (params.getAlpha() != RenderPhase.ZERO_ALPHA) {
			final AccessTexture tex = (AccessTexture) params.getTexture();
			return tex.getMipmap() ? BlendMode.CUTOUT_MIPPED : BlendMode.CUTOUT;
		} else {
			return BlendMode.SOLID;
		}
	}

	private static void copyFromLayer(MaterialFinderImpl finder, RenderLayer layer) {
		final AccessMultiPhaseParameters params = ((MultiPhaseExt) layer).canvas_phases();
		final AccessTexture tex = (AccessTexture) params.getTexture();

		finder.sorted(((RenderLayerExt) layer).canvas_isTranslucent());
		finder.primitive(GL11.GL_QUADS);
		finder.texture(tex.getId().orElse(null));
		finder.transparency(MaterialTransparency.fromPhase(params.getTransparency()));
		finder.depthTest(MaterialDepthTest.fromPhase(params.getDepthTest()));
		finder.cull(params.getCull() == RenderPhase.ENABLE_CULLING);
		finder.writeMask(MaterialWriteMask.fromPhase(params.getWriteMaskState()));
		finder.enableLightmap(params.getLightmap() == RenderPhase.ENABLE_LIGHTMAP);
		finder.decal(MaterialDecal.fromPhase(params.getLayering()));
		finder.target(MaterialTarget.fromPhase(params.getTarget()));
		finder.lines(params.getLineWidth() != RenderPhase.FULL_LINE_WIDTH);
		finder.fog(MaterialFog.fromPhase(params.getFog()));
		finder.unmipped(!tex.getMipmap());
		finder.blur(tex.getBilinear());
		finder.cutout(params.getAlpha() != RenderPhase.ZERO_ALPHA);
		finder.transparentCutout(params.getAlpha() == RenderPhase.ONE_TENTH_ALPHA);

		// vanilla sets these as part of draw process but we don't want special casing
		if (layer ==  RenderLayer.getSolid() || layer == RenderLayer.getCutoutMipped() || layer == RenderLayer.getCutout() || layer == RenderLayer.getTranslucent()) {
			finder.cull(true);
			finder.texture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
			finder.writeMask(MaterialFinder.WRITE_MASK_COLOR_DEPTH);
			finder.enableLightmap(true);
			finder.disableAo(false);
			finder.disableDiffuse(false);
		} else {
			finder.disableAo(true);
			finder.disableDiffuse(params.getDiffuseLighting() == RenderPhase.DISABLE_DIFFUSE_LIGHTING);
		}
	}

	private static final ObjectOpenHashSet<String> VANILLA_MATERIAL_SET = new ObjectOpenHashSet<>();

	public static RenderMaterialImpl copyFromLayer(RenderLayer layer) {
		if (isExcluded(layer)) {
			return RenderMaterialImpl.MISSING;
		}

		final String name = ((MultiPhaseExt) layer).canvas_name();

		if (name.equals("end_portal")) {
			EXCLUSIONS.add(layer);
			return RenderMaterialImpl.MISSING;
		}

		final MaterialFinderImpl finder = MaterialFinderImpl.threadLocal();
		copyFromLayer(finder, layer);

		finder.renderlayerName(name);

		final RenderMaterialImpl result = finder.find();

		if (Configurator.logMaterials) {
			final String key = name +": " + layer.toString();

			if(VANILLA_MATERIAL_SET.add(key)) {
				CanvasMod.LOG.info("Encountered new unique RenderLayer\n"
				+ key + "\n"
				+ "primary target transparency: " + result.primaryTargetTransparency + "\n"
				+ "mapped to render material #" + result.index + "\n");
			}
		}

		return result;
	}

	// PERF: disable translucent sorting on vanilla layers that don't actually require it - like horse spots
	// may need to be a lookup table because some will need it.

	public static final RenderMaterialImpl TRANSLUCENT_TERRAIN = copyFromLayer(RenderLayer.getTranslucent());
	public static final RenderMaterialImpl TRANSLUCENT_ITEM_ENTITY = copyFromLayer(TexturedRenderLayers.getItemEntityTranslucentCull());

	static {
		assert TRANSLUCENT_TERRAIN.primaryTargetTransparency;
		assert TRANSLUCENT_ITEM_ENTITY.primaryTargetTransparency;
	}
}
