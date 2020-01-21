package grondag.canvas.buffer.allocation;

import org.lwjgl.opengl.GL21;

public class BindStateManager {
	private static int lastBoundBufferId = -1;

	public static boolean bind(int glBufferId) {
		if(glBufferId == lastBoundBufferId) {
			return false;
		} else {
			lastBoundBufferId = glBufferId;
			GL21.glBindBuffer(GL21.GL_ARRAY_BUFFER, glBufferId);
			return true;
		}
	}

	public static void unbind() {
		if(lastBoundBufferId != -1) {
			lastBoundBufferId = -1;
			GL21.glBindBuffer(GL21.GL_ARRAY_BUFFER, 0);
		}
	}
}
