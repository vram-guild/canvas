package grondag.canvas.buffer.allocation;

import com.mojang.blaze3d.platform.GLX;

public class BindStateManager {
	private static int lastBoundBufferId = -1;

	public static boolean bind(int glBufferId) {
		if(glBufferId == lastBoundBufferId) {
			return false;
		} else {
			lastBoundBufferId = glBufferId;
			GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, glBufferId);
			return true;
		}
	}

	public static void unbind() {
		if(lastBoundBufferId != -1) {
			lastBoundBufferId = -1;
			GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, 0);
		}
	}
}
