/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.shader.data;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import io.vram.frex.api.math.FastMatrix4f;
import io.vram.frex.api.math.FastMatrix3f;

import grondag.canvas.mixinterface.GameRendererExt;

public final class MatrixData {
	private MatrixData() { }

	private static final Matrix3f IDENTITY = new Matrix3f();

	static {
		IDENTITY.setIdentity();
	}

	static void update(PoseStack.Pose view, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
		// write values for prior frame before updating
		viewMatrixExt.f_writeToBuffer(VIEW_LAST * 16, MATRIX_DATA);
		projMatrixExt.f_writeToBuffer(PROJ_LAST * 16, MATRIX_DATA);
		viewProjMatrixExt.f_writeToBuffer(VP_LAST * 16, MATRIX_DATA);
		cleanProjMatrixExt.f_writeToBuffer(CLEAN_PROJ_LAST * 16, MATRIX_DATA);
		cleanViewProjMatrixExt.f_writeToBuffer(CLEAN_VP_LAST * 16, MATRIX_DATA);

		((FastMatrix3f) (Object) viewNormalMatrix).f_set((FastMatrix3f) (Object) view.normal());

		viewMatrixExt.f_set((FastMatrix4f) (Object) view.pose());
		viewMatrixExt.f_writeToBuffer(VIEW * 16, MATRIX_DATA);
		projMatrixExt.f_set((FastMatrix4f) (Object) projectionMatrix);
		projMatrixExt.f_writeToBuffer(PROJ * 16, MATRIX_DATA);

		viewMatrixInvExt.f_set(viewMatrixExt);
		// reliable inversion of rotation matrix
		viewMatrixInv.transpose();
		viewMatrixInvExt.f_writeToBuffer(VIEW_INVERSE * 16, MATRIX_DATA);

		projMatrixInvExt.f_set(projMatrixExt);
		projMatrixInv.invert();
		projMatrixInvExt.f_writeToBuffer(PROJ_INVERSE * 16, MATRIX_DATA);

		viewProjMatrixExt.f_set(projMatrixExt);
		viewProjMatrixExt.f_mul(viewMatrixExt);
		viewProjMatrixExt.f_writeToBuffer(VP * 16, MATRIX_DATA);

		viewProjMatrixInvExt.f_set(viewMatrixInvExt);
		viewProjMatrixInvExt.f_mul(projMatrixInvExt);
		viewProjMatrixInvExt.f_writeToBuffer(VP_INVERSE * 16, MATRIX_DATA);

		computeCleanProjection(camera, tickDelta);
		cleanProjMatrixExt.f_writeToBuffer(CLEAN_PROJ * 16, MATRIX_DATA);
		cleanProjMatrixInvExt.f_writeToBuffer(CLEAN_PROJ_INVERSE * 16, MATRIX_DATA);

		cleanViewProjMatrixExt.f_set(cleanProjMatrixExt);
		cleanViewProjMatrixExt.f_mul(viewMatrixExt);
		cleanViewProjMatrixExt.f_writeToBuffer(CLEAN_VP * 16, MATRIX_DATA);

		cleanViewProjMatrixInvExt.f_set(viewMatrixInvExt);
		cleanViewProjMatrixInvExt.f_mul(cleanProjMatrixInvExt);
		cleanViewProjMatrixInvExt.f_writeToBuffer(CLEAN_VP_INVERSE * 16, MATRIX_DATA);

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
			cleanProjMatrixExt.f_translate(gx.canvas_zoomX(), -gx.canvas_zoomY(), 0.0f);
			cleanProjMatrixExt.f_scale(zoom, zoom, 1.0F);
		}

		cleanProjMatrix.multiply(Matrix4f.perspective(gx.canvas_getFov(camera, tickDelta, true), mc.getWindow().getWidth() / mc.getWindow().getHeight(), 0.05F, mc.gameRenderer.getRenderDistance()));

		cleanProjMatrixInvExt.f_set(cleanProjMatrixExt);
		cleanProjMatrixInv.invert();
	}

	public static final Matrix4f viewMatrix = new Matrix4f();
	public static final FastMatrix4f viewMatrixExt = (FastMatrix4f) (Object) viewMatrix;
	private static final Matrix4f viewMatrixInv = new Matrix4f();
	private static final FastMatrix4f viewMatrixInvExt = (FastMatrix4f) (Object) viewMatrixInv;

	public static final Matrix4f projMatrix = new Matrix4f();
	public static final FastMatrix4f projMatrixExt = (FastMatrix4f) (Object) projMatrix;
	private static final Matrix4f projMatrixInv = new Matrix4f();
	private static final FastMatrix4f projMatrixInvExt = (FastMatrix4f) (Object) projMatrixInv;

	private static final Matrix4f viewProjMatrix = new Matrix4f();
	private static final FastMatrix4f viewProjMatrixExt = (FastMatrix4f) (Object) viewProjMatrix;
	private static final Matrix4f viewProjMatrixInv = new Matrix4f();
	private static final FastMatrix4f viewProjMatrixInvExt = (FastMatrix4f) (Object) viewProjMatrixInv;

	public static final Matrix4f cleanProjMatrix = new Matrix4f();
	public static final FastMatrix4f cleanProjMatrixExt = (FastMatrix4f) (Object) cleanProjMatrix;
	private static final Matrix4f cleanProjMatrixInv = new Matrix4f();
	private static final FastMatrix4f cleanProjMatrixInvExt = (FastMatrix4f) (Object) cleanProjMatrixInv;

	private static final Matrix4f cleanViewProjMatrix = new Matrix4f();
	private static final FastMatrix4f cleanViewProjMatrixExt = (FastMatrix4f) (Object) cleanViewProjMatrix;
	private static final Matrix4f cleanViewProjMatrixInv = new Matrix4f();
	private static final FastMatrix4f cleanViewProjMatrixInvExt = (FastMatrix4f) (Object) cleanViewProjMatrixInv;

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
