package grondag.canvas.terrain.render;

import java.util.concurrent.ArrayBlockingQueue;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.material.MaterialState;

public class DrawableDelegate {
	private static final ArrayBlockingQueue<DrawableDelegate> store = new ArrayBlockingQueue<>(4096);

	public static DrawableDelegate claim(MaterialState renderState, int vertexOffset, int vertexCount) {
		DrawableDelegate result = store.poll();

		if (result == null) {
			result = new DrawableDelegate();
		}

		result.materialState = renderState;
		result.vertexOffset = vertexOffset;
		result.vertexCount = vertexCount;
		result.isReleased = false;
		return result;
	}

	private MaterialState materialState;
	private int vertexOffset;
	private int vertexCount;
	private boolean isReleased = false;

	private DrawableDelegate() {
		super();
	}

	/**
	 * The pipeline (and vertex format) associated with this delegate.
	 */
	public MaterialState materialState() {
		return materialState;
	}

	/**
	 * Assumes pipeline has already been activated and buffer has already been bound
	 * via {@link #bind()}
	 */
	public void draw() {
		assert !isReleased;

		GlStateManager.drawArrays(GL11.GL_QUADS, vertexOffset, vertexCount);
	}

	public void release() {
		assert RenderSystem.isOnRenderThread();

		if (!isReleased) {
			isReleased = true;
			materialState =  null;
			store.offer(this);
		}
	}

	public int vertexCount() {
		return vertexCount;
	}
}
