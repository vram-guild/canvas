/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
 ******************************************************************************/


package grondag.canvas.apiimpl.rendercontext;

import java.util.Random;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.buffer.encoding.VertexEncoders;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.frex.api.material.MaterialMap;

public abstract class AbstractRenderContext implements RenderContext {
	public final float[] vecData = new float[3];
	public final int[] appendData  = new int[MaterialVertexFormats.MAX_QUAD_INT_STRIDE];
	public final VertexCollectorList collectors = new VertexCollectorList();
	private final ObjectArrayList<QuadTransform> transformStack = new ObjectArrayList<>();
	private static final QuadTransform NO_TRANSFORM = (q) -> true;
	private final SpriteAtlasTexture atlas = MinecraftClient.getInstance().getBakedModelManager().method_24153(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
	private final SpriteFinder spriteFinder = SpriteFinder.get(atlas);
	private static final MaterialMap defaultMap = MaterialMap.defaultMaterialMap();

	protected Matrix4f matrix;
	protected Matrix3fExt normalMatrix;
	protected int overlay;
	protected MaterialMap materialMap = defaultMap;
	protected boolean isFluidModel = false;

	private final QuadTransform stackTransform = (q) -> {
		int i = transformStack.size() - 1;

		while (i >= 0) {
			if (!transformStack.get(i--).transform(q)) {
				return false;
			}
		}

		return true;
	};

	private QuadTransform activeTransform = NO_TRANSFORM;

	protected final boolean transform(MutableQuadView q) {
		return activeTransform.transform(q);
	}

	protected boolean hasTransform() {
		return activeTransform != NO_TRANSFORM;
	}

	void mapMaterials(MutableQuadView quad) {
		if (isFluidModel || materialMap == defaultMap) {
			return;
		}

		final Sprite sprite = materialMap.needsSprite() ? spriteFinder.find(quad, 0) : null;
		final RenderMaterial mapped = materialMap.getMapped(sprite);

		if (mapped != null) {
			quad.material(mapped);
		}
	}

	@Override
	public void pushTransform(QuadTransform transform) {
		if (transform == null) {
			throw new NullPointerException("Renderer received null QuadTransform.");
		}

		transformStack.push(transform);

		if (transformStack.size() == 1) {
			activeTransform = transform;
		} else if (transformStack.size() == 2) {
			activeTransform = stackTransform;
		}
	}

	@Override
	public void popTransform() {
		transformStack.pop();

		if (transformStack.size() == 0) {
			activeTransform = NO_TRANSFORM;
		} else if (transformStack.size() == 1) {
			activeTransform = transformStack.get(0);
		}
	}

	protected final MeshConsumer meshConsumer = new MeshConsumer(this);

	@Override
	public final Consumer<Mesh> meshConsumer() {
		return meshConsumer;
	}

	protected final FallbackConsumer fallbackConsumer = new FallbackConsumer(this);

	@Override
	public final Consumer<BakedModel> fallbackConsumer() {
		return fallbackConsumer;
	}

	@Override
	public final QuadEmitter getEmitter() {
		return meshConsumer.getEmitter();
	}

	protected boolean cullTest(MutableQuadViewImpl quad) {
		return true;
	}

	protected abstract Random random();

	public abstract boolean defaultAo();

	protected abstract BlockState blockState();

	public abstract MaterialContext materialContext();

	public abstract VertexConsumer consumer(DrawableMaterial mat);

	public abstract int indexedColor(int colorIndex);

	/**
	 * Used in contexts with a fixed brightness, like ITEM.
	 */
	public abstract int brightness();

	/**
	 * Null in some contexts, like ITEM.
	 */
	public abstract @Nullable AoCalculator aoCalc();

	public abstract int flatBrightness(MutableQuadViewImpl quad);

	public final int overlay() {
		return overlay;
	}

	public final Matrix4f matrix() {
		return matrix;
	}

	public final Matrix3fExt normalMatrix() {
		return normalMatrix;
	}

	protected abstract int defaultBlendModeIndex();

	//[17:37:00] [Canvas Render Thread - 6/INFO] (Canvas) Avg renderQuad duration = 2,345 ns, min = 416, max = 6961433, total duration = 234, total runs = 100,000
	//[17:37:01] [Canvas Render Thread - 4/INFO] (Canvas) Avg renderQuad duration = 2,378 ns, min = 423, max = 5694429, total duration = 237, total runs = 100,000
	//[17:37:01] [Canvas Render Thread - 5/INFO] (Canvas) Avg renderQuad duration = 2,452 ns, min = 421, max = 15324840, total duration = 245, total runs = 100,000
	//[17:37:01] [Canvas Render Thread - 2/INFO] (Canvas) Avg renderQuad duration = 2,356 ns, min = 410, max = 8633303, total duration = 235, total runs = 100,000
	//[17:37:01] [Canvas Render Thread - 0/INFO] (Canvas) Avg renderQuad duration = 2,455 ns, min = 412, max = 4284955, total duration = 245, total runs = 100,000
	//[17:37:01] [Canvas Render Thread - 1/INFO] (Canvas) Avg renderQuad duration = 2,393 ns, min = 448, max = 4877397, total duration = 239, total runs = 100,000
	//[17:37:02] [Canvas Render Thread - 3/INFO] (Canvas) Avg renderQuad duration = 2,541 ns, min = 460, max = 9922941, total duration = 254, total runs = 100,000
	//[17:37:03] [Canvas Render Thread - 5/INFO] (Canvas) Avg renderQuad duration = 2,939 ns, min = 442, max = 59084081, total duration = 293, total runs = 100,000
	//[17:37:03] [Canvas Render Thread - 4/INFO] (Canvas) Avg renderQuad duration = 2,640 ns, min = 437, max = 7593792, total duration = 264, total runs = 100,000
	//[17:37:04] [main/INFO] (Canvas) Avg renderQuad duration = 1,702 ns, min = 838, max = 47463, total duration = 170, total runs = 100,000

	//[18:49:14] [Canvas Render Thread - 3/INFO] (Canvas) Avg renderQuad duration = 2,479 ns, min = 352, max = 14515039, total duration = 247, total runs = 100,000
	//[18:49:14] [Canvas Render Thread - 1/INFO] (Canvas) Avg renderQuad duration = 2,472 ns, min = 352, max = 11971371, total duration = 247, total runs = 100,000
	//[18:49:14] [Canvas Render Thread - 5/INFO] (Canvas) Avg renderQuad duration = 2,202 ns, min = 345, max = 12305123, total duration = 220, total runs = 100,000
	//[18:49:16] [Canvas Render Thread - 6/INFO] (Canvas) Avg renderQuad duration = 2,534 ns, min = 365, max = 12819764, total duration = 253, total runs = 100,000
	//[18:49:16] [Canvas Render Thread - 2/INFO] (Canvas) Avg renderQuad duration = 2,059 ns, min = 369, max = 7602219, total duration = 205, total runs = 100,000
	//[18:49:18] [Canvas Render Thread - 0/INFO] (Canvas) Avg renderQuad duration = 2,141 ns, min = 365, max = 5758216, total duration = 214, total runs = 100,000
	//[18:49:19] [Canvas Render Thread - 4/INFO] (Canvas) Avg renderQuad duration = 2,071 ns, min = 343, max = 8764804, total duration = 207, total runs = 100,000
	//[18:49:20] [main/INFO] (Canvas) Avg renderQuad duration = 1,556 ns, min = 659, max = 56858, total duration = 155, total runs = 100,000
	//[18:49:27] [main/INFO] (Canvas) Avg renderQuad duration = 1,628 ns, min = 583, max = 82779, total duration = 162, total runs = 100,000

	//[10:45:07] [main/INFO] (Canvas) Avg renderQuad duration = 3,666 ns, min = 1332, max = 165333, total duration = 366, total runs = 100,000
	//[10:45:08] [Canvas Render Thread - 6/INFO] (Canvas) Avg renderQuad duration = 1,164 ns, min = 4, max = 5060948, total duration = 116, total runs = 100,000
	//[10:45:08] [Canvas Render Thread - 5/INFO] (Canvas) Avg renderQuad duration = 1,167 ns, min = 9, max = 6202852, total duration = 116, total runs = 100,000
	//[10:45:08] [Canvas Render Thread - 4/INFO] (Canvas) Avg renderQuad duration = 1,100 ns, min = 5, max = 5377311, total duration = 110, total runs = 100,000
	//[10:45:09] [Canvas Render Thread - 2/INFO] (Canvas) Avg renderQuad duration = 892 ns, min = 15, max = 1766698, total duration = 89, total runs = 100,000
	//[10:45:10] [Canvas Render Thread - 1/INFO] (Canvas) Avg renderQuad duration = 1,282 ns, min = 3, max = 6586611, total duration = 128, total runs = 100,000
	//[10:45:10] [Canvas Render Thread - 0/INFO] (Canvas) Avg renderQuad duration = 1,249 ns, min = 5, max = 4150995, total duration = 124, total runs = 100,000
	//[10:45:10] [Canvas Render Thread - 3/INFO] (Canvas) Avg renderQuad duration = 1,269 ns, min = 2, max = 4476963, total duration = 126, total runs = 100,000
	//[10:45:10] [Canvas Render Thread - 4/INFO] (Canvas) Avg renderQuad duration = 897 ns, min = 21, max = 7570932, total duration = 89, total runs = 100,000
	//[10:45:10] [Canvas Render Thread - 6/INFO] (Canvas) Avg renderQuad duration = 1,398 ns, min = 2, max = 5779330, total duration = 139, total runs = 100,000

	// TODO: remove
	//	private final MicroTimer timer = new MicroTimer("renderQuad", 100000);

	public final void renderQuad(MutableQuadViewImpl quad) {
		//		timer.start();

		mapMaterials(quad);

		if (transform(quad) && cullTest(quad)) {
			final CompositeMaterial mat = quad.material().forBlendMode(defaultBlendModeIndex());
			quad.material(mat);
			VertexEncoders.get(materialContext(), mat).encodeQuad(quad, this);
		}

		//		timer.stop();
	}
}
