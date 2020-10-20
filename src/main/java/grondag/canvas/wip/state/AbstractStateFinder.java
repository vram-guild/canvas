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
import grondag.canvas.wip.shader.WipMaterialShaderManager;
import grondag.canvas.wip.shader.WipShaderData;
import grondag.canvas.wip.state.property.WipDecal;
import grondag.canvas.wip.state.property.WipDepthTest;
import grondag.canvas.wip.state.property.WipFog;
import grondag.canvas.wip.state.property.WipTarget;
import grondag.canvas.wip.state.property.WipTextureState;
import grondag.canvas.wip.state.property.WipTransparency;
import grondag.canvas.wip.state.property.WipWriteMask;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import static grondag.canvas.wip.state.AbstractRenderState.BILINEAR;
import static grondag.canvas.wip.state.AbstractRenderState.CULL;
import static grondag.canvas.wip.state.AbstractRenderState.DECAL;
import static grondag.canvas.wip.state.AbstractRenderState.DEPTH_TEST;
import static grondag.canvas.wip.state.AbstractRenderState.ENABLE_LIGHTMAP;
import static grondag.canvas.wip.state.AbstractRenderState.FOG;
import static grondag.canvas.wip.state.AbstractRenderState.FRAGMENT_SHADER;
import static grondag.canvas.wip.state.AbstractRenderState.LINES;
import static grondag.canvas.wip.state.AbstractRenderState.PACKER;
import static grondag.canvas.wip.state.AbstractRenderState.PRIMITIVE;
import static grondag.canvas.wip.state.AbstractRenderState.TARGET;
import static grondag.canvas.wip.state.AbstractRenderState.TEXTURE;
import static grondag.canvas.wip.state.AbstractRenderState.TRANSPARENCY;
import static grondag.canvas.wip.state.AbstractRenderState.VERTEX_SHADER;
import static grondag.canvas.wip.state.AbstractRenderState.WRITE_MASK;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.util.Identifier;

@SuppressWarnings("unchecked")
public abstract class AbstractStateFinder<T extends AbstractStateFinder<T, V>, V extends AbstractRenderState> {
	protected long bits;

	public T reset() {
		bits = 0;
		vertexShader(WipShaderData.DEFAULT_VERTEX_SOURCE);
		fragmentShader(WipShaderData.DEFAULT_FRAGMENT_SOURCE);
		return (T) this;
	}

	public T primitive(int primitive) {
		assert primitive <= 7;
		bits = PRIMITIVE.setValue(primitive, bits);
		return (T) this;
	}

	//		private static final Identifier EGREGIOUS_ENDERMAN_HACK = new Identifier("textures/entity/enderman/enderman.png");

	public T texture(@Nullable Identifier id) {
		final int val = id == null ? WipTextureState.NO_TEXTURE.index : WipTextureState.fromId(id).index;
		bits = TEXTURE.setValue(val, bits);

		// WIP2: put in proper material map hooks
		//			if (id != null && id.equals(EGREGIOUS_ENDERMAN_HACK)) {
		//				fragmentShader(new Identifier("canvas:shaders/wip/material/enderman.frag"));
		//			}

		return (T) this;
	}

	public T bilinear(boolean bilinear) {
		bits = BILINEAR.setValue(bilinear, bits);
		return (T) this;
	}

	public T transparency(WipTransparency transparency) {
		bits = TRANSPARENCY.setValue(transparency, bits);
		return (T) this;
	}

	public T depthTest(WipDepthTest depthTest) {
		bits = DEPTH_TEST.setValue(depthTest, bits);
		return (T) this;
	}

	public T cull(boolean cull) {
		bits = CULL.setValue(cull, bits);
		return (T) this;
	}

	public T writeMask(WipWriteMask writeMask) {
		bits = WRITE_MASK.setValue(writeMask, bits);
		return (T) this;
	}

	public T enableLightmap(boolean enableLightmap) {
		bits = ENABLE_LIGHTMAP.setValue(enableLightmap, bits);
		return (T) this;
	}

	public T decal(WipDecal decal) {
		bits = DECAL.setValue(decal, bits);
		return (T) this;
	}

	public T target(WipTarget target) {
		bits = TARGET.setValue(target, bits);
		return (T) this;
	}

	public T lines(boolean lines) {
		bits = LINES.setValue(lines, bits);
		return (T) this;
	}

	public T fog(WipFog fog) {
		bits = FOG.setValue(fog, bits);
		return (T) this;
	}

	public T vertexShader(Identifier vertexSource) {
		bits = VERTEX_SHADER.setValue(WipMaterialShaderManager.vertexIndex.toHandle(vertexSource), bits);
		return (T) this;
	}

	public T fragmentShader(Identifier fragmentSource) {
		bits = FRAGMENT_SHADER.setValue(WipMaterialShaderManager.fragmentIndex.toHandle(fragmentSource), bits);
		return (T) this;
	}

	protected abstract V missing();

	protected abstract V find();

	public V copyFromLayer(RenderLayer layer) {
		if (AbstractStateFinder.isExcluded(layer)) {
			return missing();
		}

		final AccessMultiPhaseParameters params = ((MultiPhaseExt) layer).canvas_phases();
		final AccessTexture tex = (AccessTexture) params.getTexture();

		primitive(GL11.GL_QUADS);
		texture(tex.getId().orElse(null));
		transparency(WipTransparency.fromPhase(params.getTransparency()));
		depthTest(WipDepthTest.fromPhase(params.getDepthTest()));
		cull(params.getCull() == RenderPhase.ENABLE_CULLING);
		writeMask(WipWriteMask.fromPhase(params.getWriteMaskState()));
		enableLightmap(params.getLightmap() == RenderPhase.ENABLE_LIGHTMAP);
		decal(WipDecal.fromPhase(params.getLayering()));
		target(WipTarget.fromPhase(params.getTarget()));
		lines(params.getLineWidth() != RenderPhase.FULL_LINE_WIDTH);
		fog(WipFog.fromPhase(params.getFog()));

		return find();
	}

	private static final ReferenceOpenHashSet<RenderLayer> EXCLUSIONS = new ReferenceOpenHashSet<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	static {
		assert PACKER.bitLength() <= 56; // high eight bits saved for RenderMaterial aggregate

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
}
