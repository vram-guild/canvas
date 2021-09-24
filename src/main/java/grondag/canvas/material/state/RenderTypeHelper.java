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
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.impl.material.RenderTypeShardHelper;
import io.vram.frex.mixin.core.AccessCompositeState;
import io.vram.frex.mixin.core.AccessTextureStateShard;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.CompositeRenderTypeExt;
import grondag.canvas.mixinterface.RenderTypeExt;
import grondag.canvas.mixinterface.ShaderStateShardExt;

// segregates render layer references from mod init
public final class RenderTypeHelper {
	private RenderTypeHelper() { }

	private static final ReferenceOpenHashSet<RenderType> EXCLUSIONS = new ReferenceOpenHashSet<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	static {
		// entity shadows aren't worth
		EXCLUSIONS.add(EntityRenderDispatcher.SHADOW_RENDER_TYPE);

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

	private static void copyRenderTypeAttributes(MaterialFinderImpl finder, CompositeRenderTypeExt type) {
		final AccessCompositeState params = type.canvas_phases();
		final EmptyTextureStateShard texBase = params.getTextureState();

		final MojangShaderData sd = ((ShaderStateShardExt) params.getShaderState()).canvas_shaderData();

		if (texBase != null && texBase instanceof AccessTextureStateShard) {
			final AccessTextureStateShard tex = (AccessTextureStateShard) params.getTextureState();
			finder.texture(tex.getTexture().orElse(null));
			finder.unmipped(!tex.getMipmap());
			finder.blur(tex.getBlur());
		}

		finder.transparency(RenderTypeShardHelper.toMaterialTransparency(params.getTransparencyState()));
		finder.depthTest(RenderTypeShardHelper.toMaterialDepthTest(params.getDepthTestState()));
		finder.cull(params.getCullState() == RenderStateShard.CULL);
		finder.writeMask(RenderTypeShardHelper.toMaterialWriteMask(params.getWriteMaskState()));
		finder.decal(RenderTypeShardHelper.toMaterialDecal(params.getLayeringState()));
		finder.target(RenderTypeShardHelper.toMaterialTarget(params.getOutputState()));
		finder.lines(params.getLineState() != RenderStateShard.DEFAULT_LINE);
		finder.fog(sd.fog);
		finder.disableDiffuse(!sd.diffuse);
		finder.cutout(sd.cutout);
		finder.sorted(((RenderTypeExt) type).canvas_isSorted());

		// vanilla sets these as part of draw process but we don't want special casing
		if (type == RenderType.solid() || type == RenderType.cutoutMipped() || type == RenderType.cutout() || type == RenderType.translucent()) {
			finder.cull(true);
			finder.texture(TextureAtlas.LOCATION_BLOCKS);
			finder.writeMask(MaterialConstants.WRITE_MASK_COLOR_DEPTH);
			finder.disableAo(false);
		} else {
			finder.disableAo(true);
		}
	}

	private static final ObjectOpenHashSet<String> VANILLA_MATERIAL_SET = new ObjectOpenHashSet<>();

	public static RenderMaterial copyFromRenderType(RenderType renderType) {
		return copyFromRenderType(renderType, false);
	}

	public static RenderMaterial copyFromRenderType(RenderType renderType, boolean glint) {
		if (isExcluded(renderType)) {
			return RenderMaterialImpl.MISSING_MATERIAL;
		}

		final CompositeRenderTypeExt multiPhase = (CompositeRenderTypeExt) renderType;
		final String name = multiPhase.canvas_name();
		final var params = multiPhase.canvas_phases();

		// Excludes glint, end portal, and other specialized render layers that won't play nice with our current setup
		// Excludes render layers with custom shaders
		if (params.getTexturingState() != RenderStateShard.DEFAULT_TEXTURING || ((ShaderStateShardExt) params.getShaderState()).canvas_shaderData() == MojangShaderData.MISSING) {
			EXCLUSIONS.add(renderType);
			return RenderMaterialImpl.MISSING_MATERIAL;
		}

		final MaterialFinderImpl finder = MaterialFinderImpl.threadLocal();
		copyRenderTypeAttributes(finder, multiPhase);
		finder.renderlayerName(name);
		finder.enableGlint(glint);
		final RenderMaterialImpl result = finder.find();

		if (Configurator.logMaterials) {
			final String key = name +": " + renderType.toString();

			if (VANILLA_MATERIAL_SET.add(key)) {
				CanvasMod.LOG.info("Encountered new unique RenderLayer\n"
					+ key + "\n"
					+ "primary target transparency: " + result.primaryTargetTransparency + "\n"
					+ "mapped to render material #" + result.index + "\n");
			}
		}

		return result;
	}
}
