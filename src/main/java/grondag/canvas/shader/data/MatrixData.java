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

package grondag.canvas.shader.data;

import java.nio.FloatBuffer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import org.lwjgl.BufferUtils;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import grondag.canvas.mixinterface.GameRendererExt;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

public final class MatrixData {
	private MatrixData() { }

	private static final Matrix3f IDENTITY = new Matrix3f();

	static {
		IDENTITY.setIdentity();
	}

	static void update(PoseStack.Pose view, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
		// write values for prior frame before updating
		viewMatrixExt.writeToBuffer(VIEW_LAST * 16, MATRIX_DATA);
		projMatrixExt.writeToBuffer(PROJ_LAST * 16, MATRIX_DATA);
		viewProjMatrixExt.writeToBuffer(VP_LAST * 16, MATRIX_DATA);
		cleanProjMatrixExt.writeToBuffer(CLEAN_PROJ_LAST * 16, MATRIX_DATA);
		cleanViewProjMatrixExt.writeToBuffer(CLEAN_VP_LAST * 16, MATRIX_DATA);

		((Matrix3fExt) (Object) viewNormalMatrix).set((Matrix3fExt) (Object) view.normal());

		viewMatrixExt.set((Matrix4fExt) (Object) view.pose());
		viewMatrixExt.writeToBuffer(VIEW * 16, MATRIX_DATA);
		projMatrixExt.set((Matrix4fExt) (Object) projectionMatrix);
		projMatrixExt.writeToBuffer(PROJ * 16, MATRIX_DATA);

		viewMatrixInvExt.set(viewMatrixExt);
		// reliable inversion of rotation matrix
		viewMatrixInv.transpose();
		viewMatrixInvExt.writeToBuffer(VIEW_INVERSE * 16, MATRIX_DATA);

		projMatrixInvExt.set(projMatrixExt);
		projMatrixInv.invert();
		projMatrixInvExt.writeToBuffer(PROJ_INVERSE * 16, MATRIX_DATA);

		viewProjMatrixExt.set(projMatrixExt);
		viewProjMatrixExt.multiply(viewMatrixExt);
		viewProjMatrixExt.writeToBuffer(VP * 16, MATRIX_DATA);

		viewProjMatrixInvExt.set(viewMatrixInvExt);
		viewProjMatrixInvExt.multiply(projMatrixInvExt);
		viewProjMatrixInvExt.writeToBuffer(VP_INVERSE * 16, MATRIX_DATA);

		computeCleanProjection(camera, tickDelta);
		cleanProjMatrixExt.writeToBuffer(CLEAN_PROJ * 16, MATRIX_DATA);
		cleanProjMatrixInvExt.writeToBuffer(CLEAN_PROJ_INVERSE * 16, MATRIX_DATA);

		cleanViewProjMatrixExt.set(cleanProjMatrixExt);
		cleanViewProjMatrixExt.multiply(viewMatrixExt);
		cleanViewProjMatrixExt.writeToBuffer(CLEAN_VP * 16, MATRIX_DATA);

		cleanViewProjMatrixInvExt.set(viewMatrixInvExt);
		cleanViewProjMatrixInvExt.multiply(cleanProjMatrixInvExt);
		cleanViewProjMatrixInvExt.writeToBuffer(CLEAN_VP_INVERSE * 16, MATRIX_DATA);

		//cleanFrustum.prepare(viewMatrix, tickDelta, camera, cleanProjMatrix);
		//cleanFrustum.computeCircumCenter(viewMatrixInv, cleanProjMatrixInv);
	}

	/**
	 * Computes projection that doesn't include nausea or view bob and doesn't have 4X depth like vanilla.
	 */
	private static void computeCleanProjection(Camera camera, float tickDelta) {
		final Minecraft mc = Minecraft.getInstance();
		final GameRendererExt gx = (GameRendererExt) mc.gameRenderer;
		final float zoom = gx.canvas_zoom();

		cleanProjMatrix.setIdentity();

		if (zoom != 1.0F) {
			cleanProjMatrixExt.translate(gx.canvas_zoomX(), -gx.canvas_zoomY(), 0.0f);
			cleanProjMatrixExt.scale(zoom, zoom, 1.0F);
		}

		cleanProjMatrix.multiply(Matrix4f.perspective(gx.canvas_getFov(camera, tickDelta, true), mc.getWindow().getWidth() / mc.getWindow().getHeight(), 0.05F, mc.gameRenderer.getRenderDistance()));

		cleanProjMatrixInvExt.set(cleanProjMatrixExt);
		cleanProjMatrixInv.invert();
	}

	public static final Matrix4f viewMatrix = new Matrix4f();
	public static final Matrix4fExt viewMatrixExt = (Matrix4fExt) (Object) viewMatrix;
	private static final Matrix4f viewMatrixInv = new Matrix4f();
	private static final Matrix4fExt viewMatrixInvExt = (Matrix4fExt) (Object) viewMatrixInv;

	public static final Matrix4f projMatrix = new Matrix4f();
	public static final Matrix4fExt projMatrixExt = (Matrix4fExt) (Object) projMatrix;
	private static final Matrix4f projMatrixInv = new Matrix4f();
	private static final Matrix4fExt projMatrixInvExt = (Matrix4fExt) (Object) projMatrixInv;

	private static final Matrix4f viewProjMatrix = new Matrix4f();
	private static final Matrix4fExt viewProjMatrixExt = (Matrix4fExt) (Object) viewProjMatrix;
	private static final Matrix4f viewProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt viewProjMatrixInvExt = (Matrix4fExt) (Object) viewProjMatrixInv;

	public static final Matrix4f cleanProjMatrix = new Matrix4f();
	public static final Matrix4fExt cleanProjMatrixExt = (Matrix4fExt) (Object) cleanProjMatrix;
	private static final Matrix4f cleanProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt cleanProjMatrixInvExt = (Matrix4fExt) (Object) cleanProjMatrixInv;

	private static final Matrix4f cleanViewProjMatrix = new Matrix4f();
	private static final Matrix4fExt cleanViewProjMatrixExt = (Matrix4fExt) (Object) cleanViewProjMatrix;
	private static final Matrix4f cleanViewProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt cleanViewProjMatrixInvExt = (Matrix4fExt) (Object) cleanViewProjMatrixInv;

	public static final Matrix3f viewNormalMatrix = new Matrix3f();

	private static final int VIEW = 0;
	private static final int VIEW_INVERSE = 1;
	private static final int VIEW_LAST = 2;
	private static final int PROJ = 3;
	private static final int PROJ_INVERSE = 4;
	private static final int PROJ_LAST = 5;
	private static final int VP = 6;
	private static final int VP_INVERSE = 7;
	private static final int VP_LAST = 8;

	static final int SHADOW_VIEW = 9;
	static final int SHADOW_VIEW_INVERSE = 10;
	// base index of cascades 0-3
	static final int SHADOW_PROJ_0 = 11;
	// base index of cascades 0-3
	static final int SHADOW_VIEW_PROJ_0 = 15;

	private static final int CLEAN_PROJ = 19;
	private static final int CLEAN_PROJ_INVERSE = 20;
	private static final int CLEAN_PROJ_LAST = 21;
	private static final int CLEAN_VP = 22;
	private static final int CLEAN_VP_INVERSE = 23;
	private static final int CLEAN_VP_LAST = 24;

	public static final int COUNT = 25;
	public static final FloatBuffer MATRIX_DATA = BufferUtils.createFloatBuffer(COUNT * 16);
}
