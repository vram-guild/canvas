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

package grondag.canvas.material.state;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.mixin.AccessMultiPhaseParameters;
import grondag.canvas.mixin.AccessTexture;
import grondag.canvas.mixinterface.EntityRenderDispatcherExt;
import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.canvas.mixinterface.ShaderExt;
import grondag.frex.api.material.MaterialFinder;

// segregates render layer references from mod init
public final class RenderLayerHelper {
	private RenderLayerHelper() { }

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
		EXCLUSIONS.add(RenderLayer.getEndPortal());

		ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((renderLayer) -> {
			EXCLUSIONS.add(renderLayer);
		});
	}

	public static boolean isExcluded(RenderLayer layer) {
		return EXCLUSIONS.contains(layer);
	}

	private static void copyFromLayer(MaterialFinderImpl finder, MultiPhaseExt layer) {
		final AccessMultiPhaseParameters params = layer.canvas_phases();
		final AccessTexture tex = (AccessTexture) params.getTexture();
		final MojangShaderData sd = ((ShaderExt) params.getField_29461()).canvas_shaderData();
		finder.sorted(layer.canvas_isTranslucent());
		//finder.primitive(GL11.GL_QUADS);
		finder.texture(tex.getId().orElse(null));
		finder.transparency(MaterialTransparency.fromPhase(params.getTransparency()));
		finder.depthTest(MaterialDepthTest.fromPhase(params.getDepthTest()));
		finder.cull(params.getCull() == RenderPhase.ENABLE_CULLING);
		finder.writeMask(MaterialWriteMask.fromPhase(params.getWriteMaskState()));
		finder.enableLightmap(params.getLightmap() == RenderPhase.ENABLE_LIGHTMAP);
		finder.decal(MaterialDecal.fromPhase(params.getLayering()));
		finder.target(MaterialTarget.fromPhase(params.getTarget()));
		finder.lines(params.getLineWidth() != RenderPhase.FULL_LINE_WIDTH);
		finder.fog(sd.fog);
		finder.unmipped(!tex.getMipmap());
		finder.blur(tex.getBilinear());
		finder.disableDiffuse(!sd.diffuse);
		finder.cutout(sd.cutout);

		// vanilla sets these as part of draw process but we don't want special casing
		if (layer == RenderLayer.getSolid() || layer == RenderLayer.getCutoutMipped() || layer == RenderLayer.getCutout() || layer == RenderLayer.getTranslucent()) {
			finder.cull(true);
			finder.texture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
			finder.writeMask(MaterialFinder.WRITE_MASK_COLOR_DEPTH);
			finder.enableLightmap(true);
			finder.disableAo(false);
		} else {
			finder.disableAo(true);
		}
	}

	private static final ObjectOpenHashSet<String> VANILLA_MATERIAL_SET = new ObjectOpenHashSet<>();

	public static RenderMaterialImpl copyFromLayer(RenderLayer layer) {
		return copyFromLayer(layer, false);
	}

	public static RenderMaterialImpl copyFromLayer(RenderLayer layer, boolean glint) {
		if (isExcluded(layer)) {
			return RenderMaterialImpl.MISSING;
		}

		final MultiPhaseExt multiPhase = (MultiPhaseExt) layer;
		final String name = multiPhase.canvas_name();

		// Excludes glint, end portal, and other specialized render layers that won't play nice with our current setup
		if (multiPhase.canvas_phases().getTexturing() != RenderPhase.DEFAULT_TEXTURING) {
			EXCLUSIONS.add(layer);
			return RenderMaterialImpl.MISSING;
		}

		final MaterialFinderImpl finder = MaterialFinderImpl.threadLocal();
		copyFromLayer(finder, multiPhase);
		finder.renderlayerName(name);
		finder.enableGlint(glint);
		final RenderMaterialImpl result = finder.find();

		if (Configurator.logMaterials) {
			final String key = name +": " + layer.toString();

			if (VANILLA_MATERIAL_SET.add(key)) {
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
