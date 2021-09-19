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

import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import io.vram.frex.api.material.MaterialConstants;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;

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
import grondag.canvas.mixinterface.RenderLayerExt;
import grondag.canvas.mixinterface.ShaderExt;

// segregates render layer references from mod init
public final class RenderLayerHelper {
	private RenderLayerHelper() { }

	private static final ReferenceOpenHashSet<RenderType> EXCLUSIONS = new ReferenceOpenHashSet<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	static {
		// entity shadows aren't worth
		EXCLUSIONS.add(((EntityRenderDispatcherExt) Minecraft.getInstance().getEntityRenderDispatcher()).canvas_shadowLayer());

		// FEAT: handle more of these with shaders
		EXCLUSIONS.add(RenderType.armorGlint());
		EXCLUSIONS.add(RenderType.armorEntityGlint());
		EXCLUSIONS.add(RenderType.glint());
		EXCLUSIONS.add(RenderType.glintDirect());
		EXCLUSIONS.add(RenderType.glintTranslucent());
		EXCLUSIONS.add(RenderType.entityGlint());
		EXCLUSIONS.add(RenderType.entityGlintDirect());
		EXCLUSIONS.add(RenderType.lines());
		EXCLUSIONS.add(RenderType.lightning());
		// draw order is important and our sorting mechanism doesn't cover
		EXCLUSIONS.add(RenderType.waterMask());
		EXCLUSIONS.add(RenderType.endPortal());
		EXCLUSIONS.add(RenderType.endGateway());

		ModelBakery.DESTROY_TYPES.forEach((renderLayer) -> {
			EXCLUSIONS.add(renderLayer);
		});
	}

	public static boolean isExcluded(RenderType layer) {
		// currently we only handle quads
		return layer.mode() != Mode.QUADS || EXCLUSIONS.contains(layer);
	}

	private static void copyFromLayer(MaterialFinderImpl finder, MultiPhaseExt layer) {
		final AccessMultiPhaseParameters params = layer.canvas_phases();
		final EmptyTextureStateShard texBase = params.getTexture();

		final MojangShaderData sd = ((ShaderExt) params.getShader()).canvas_shaderData();

		if (texBase != null && texBase instanceof AccessTexture) {
			final AccessTexture tex = (AccessTexture) params.getTexture();
			finder.texture(tex.getId().orElse(null));
			finder.unmipped(!tex.getMipmap());
			finder.blur(tex.getBlur());
		}

		finder.transparency(MaterialTransparency.fromPhase(params.getTransparency()));
		finder.depthTest(MaterialDepthTest.fromPhase(params.getDepthTest()));
		finder.cull(params.getCull() == RenderStateShard.CULL);
		finder.writeMask(MaterialWriteMask.fromPhase(params.getWriteMaskState()));
		finder.decal(MaterialDecal.fromPhase(params.getLayering()));
		finder.target(MaterialTarget.fromPhase(params.getTarget()));
		finder.lines(params.getLineWidth() != RenderStateShard.DEFAULT_LINE);
		finder.fog(sd.fog);
		finder.disableDiffuse(!sd.diffuse);
		finder.cutout(sd.cutout);
		finder.sorted(((RenderLayerExt) layer).canvas_isTranslucent());

		// vanilla sets these as part of draw process but we don't want special casing
		if (layer == RenderType.solid() || layer == RenderType.cutoutMipped() || layer == RenderType.cutout() || layer == RenderType.translucent()) {
			finder.cull(true);
			finder.texture(TextureAtlas.LOCATION_BLOCKS);
			finder.writeMask(MaterialConstants.WRITE_MASK_COLOR_DEPTH);
			finder.disableAo(false);
		} else {
			finder.disableAo(true);
		}
	}

	private static final ObjectOpenHashSet<String> VANILLA_MATERIAL_SET = new ObjectOpenHashSet<>();

	public static RenderMaterialImpl copyFromLayer(RenderType layer) {
		return copyFromLayer(layer, false);
	}

	public static RenderMaterialImpl copyFromLayer(RenderType layer, boolean glint) {
		if (isExcluded(layer)) {
			return RenderMaterialImpl.MISSING;
		}

		final MultiPhaseExt multiPhase = (MultiPhaseExt) layer;
		final String name = multiPhase.canvas_name();
		final var params = multiPhase.canvas_phases();

		// Excludes glint, end portal, and other specialized render layers that won't play nice with our current setup
		// Excludes render layers with custom shaders
		if (params.getTexturing() != RenderStateShard.DEFAULT_TEXTURING || ((ShaderExt) params.getShader()).canvas_shaderData() == MojangShaderData.MISSING) {
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

	public static final RenderMaterialImpl TRANSLUCENT_TERRAIN = copyFromLayer(RenderType.translucent());
	public static final RenderMaterialImpl TRANSLUCENT_ITEM_ENTITY = copyFromLayer(Sheets.translucentItemSheet());

	static {
		assert TRANSLUCENT_TERRAIN.primaryTargetTransparency;
		assert TRANSLUCENT_ITEM_ENTITY.primaryTargetTransparency;
	}
}
