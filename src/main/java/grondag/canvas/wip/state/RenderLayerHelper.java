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
package grondag.canvas.wip.state;

import grondag.canvas.mixin.AccessMultiPhaseParameters;
import grondag.canvas.mixin.AccessTexture;
import grondag.canvas.mixinterface.EntityRenderDispatcherExt;
import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.canvas.wip.state.property.WipDecal;
import grondag.canvas.wip.state.property.WipDepthTest;
import grondag.canvas.wip.state.property.WipFog;
import grondag.canvas.wip.state.property.WipTarget;
import grondag.canvas.wip.state.property.WipTransparency;
import grondag.canvas.wip.state.property.WipWriteMask;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.model.ModelLoader;

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

		ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((renderLayer) -> {
			EXCLUSIONS.add(renderLayer);
		});
	}

	public static boolean isExcluded(RenderLayer layer) {
		return EXCLUSIONS.contains(layer);
	}

	public static WipRenderMaterial copyFromLayer(RenderLayer layer) {
		if (isExcluded(layer)) {
			return WipRenderMaterial.MISSING;
		}

		final AccessMultiPhaseParameters params = ((MultiPhaseExt) layer).canvas_phases();
		final AccessTexture tex = (AccessTexture) params.getTexture();

		final WipRenderMaterialFinder finder = WipRenderMaterialFinder.threadLocal();

		finder.primitive(GL11.GL_QUADS);
		finder.texture(tex.getId().orElse(null));
		finder.transparency(WipTransparency.fromPhase(params.getTransparency()));
		finder.depthTest(WipDepthTest.fromPhase(params.getDepthTest()));
		finder.cull(params.getCull() == RenderPhase.ENABLE_CULLING);
		finder.writeMask(WipWriteMask.fromPhase(params.getWriteMaskState()));
		finder.enableLightmap(params.getLightmap() == RenderPhase.ENABLE_LIGHTMAP);
		finder.decal(WipDecal.fromPhase(params.getLayering()));
		finder.target(WipTarget.fromPhase(params.getTarget()));
		finder.lines(params.getLineWidth() != RenderPhase.FULL_LINE_WIDTH);
		finder.fog(WipFog.fromPhase(params.getFog()));
		finder.unmipped(!tex.getMipmap());
		finder.disableDiffuse(params.getDiffuseLighting() == RenderPhase.DISABLE_DIFFUSE_LIGHTING);
		finder.cutout(params.getAlpha() != RenderPhase.ZERO_ALPHA);
		finder.translucentCutout(params.getAlpha() == RenderPhase.ONE_TENTH_ALPHA);
		finder.disableAo(true);

		// WIP2: put in proper material map hooks
		final String name = ((MultiPhaseExt) layer).canvas_name();
		finder.emissive(name.equals("eyes") || name.equals("beacon_beam"));

		return finder.find();
	}

	public static final WipRenderMaterial TRANSLUCENT_TERRAIN = copyFromLayer(RenderLayer.getTranslucent());
}
