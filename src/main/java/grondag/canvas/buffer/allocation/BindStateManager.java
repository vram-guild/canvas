package grondag.canvas.buffer.allocation;

import org.lwjgl.opengl.GL21;

public class BindStateManager {
	private static int boundBufferId = -1;

	public static boolean bind(int glBufferId) {
		if(glBufferId == boundBufferId) {
			return false;
		} else {
			boundBufferId = glBufferId;
			GL21.glBindBuffer(GL21.GL_ARRAY_BUFFER, glBufferId);
			return true;
		}
	}

	public static void unbind() {
		if(boundBufferId != -1) {
			boundBufferId = -1;
			GL21.glBindBuffer(GL21.GL_ARRAY_BUFFER, 0);
		}
	}
	
	public static int boundBufferId() {
		return boundBufferId;
	}
}
