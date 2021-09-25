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

package grondag.canvas.wip;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.material.RenderTypeExclusion;
import io.vram.frex.api.renderer.Renderer;
import io.vram.frex.impl.material.MojangShaderData;
import io.vram.frex.impl.material.RenderTypeShardHelper;
import io.vram.frex.mixin.core.AccessCompositeState;
import io.vram.frex.mixin.core.AccessTextureStateShard;
import org.jetbrains.annotations.ApiStatus.Internal;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

import grondag.canvas.mixinterface.CompositeRenderTypeExt;
import grondag.canvas.mixinterface.ShaderStateShardExt;

@Internal
public final class RenderTypeUtilImpl {
	private RenderTypeUtilImpl() { }

	public static boolean toMaterialFinder(MaterialFinder finder, RenderType renderType) {
		if (RenderTypeExclusion.isExcluded(renderType)) {
			return false;
		}

		copyRenderTypeAttributes(finder, renderType);
		return true;
	}

	private static void copyRenderTypeAttributes(MaterialFinder finder, RenderType renderType) {
		final AccessCompositeState params = ((CompositeRenderTypeExt) renderType).canvas_phases();
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
		finder.sorted(renderType.sortOnUpload);
		finder.label(renderType.name);

		// vanilla sets these as part of draw process but we don't want special casing
		if (renderType == RenderType.solid() || renderType == RenderType.cutoutMipped() || renderType == RenderType.cutout() || renderType == RenderType.translucent()) {
			finder.cull(true);
			finder.texture(TextureAtlas.LOCATION_BLOCKS);
			finder.writeMask(MaterialConstants.WRITE_MASK_COLOR_DEPTH);
			finder.disableAo(false);
		} else {
			finder.disableAo(true);
		}
	}

	public static RenderMaterial toMaterial(RenderType renderType, boolean foilOverlay) {
		if (RenderTypeExclusion.isExcluded(renderType)) {
			return Renderer.get().materialById(RenderMaterial.MISSING_MATERIAL_KEY);
		}

		final MaterialFinder finder = MaterialFinder.threadLocal();
		copyRenderTypeAttributes(finder, renderType);
		finder.foilOverlay(foilOverlay);
		final RenderMaterial result = finder.find();

		return result;
	}
}
