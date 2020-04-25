package grondag.canvas.render;

public class Visibility {
	private  Visibility() {}


	/**
	 * Incremented when player moves more than 1 block.
	 * Triggers visibility rebuild and translucency resort.
	 */
	private static int viewPositionVersion;

	private static int viewRotationVersion;


	//	private static double viewX;
	//	private static double viewY;
	//	private static double viewZ;
	//
	//	private double lastCameraX;
	//	private double lastCameraY;
	//	private double lastCameraZ;
	//	private double lastCameraPitch;
	//	private double lastCameraYaw;



	//	public static int regionPositionVersion() {
	//		return regionPositionVersion;
	//	}

	//	//  TODO: remove
	//	public int canvas_camereChunkX() {
	//		return playerChunkX;
	//	}
	//	//  TODO: remove
	//	public int canvas_camereChunkY() {
	//		return cameraChunkY;
	//	}
	//	//  TODO: remove
	//	public int canvas_camereChunkZ() {
	//		return cameraChunkZ;
	//	}
	//
	//	//  TODO: remove
	//	public boolean canvas_checkNeedsTerrainUpdate(Vec3d cameraPos, float pitch, float yaw) {
	//		needsTerrainUpdate = needsTerrainUpdate || !canvasWorldRenderer.chunksToRebuild.isEmpty() || cameraPos.x != lastCameraX || cameraPos.y != lastCameraY || cameraPos.z != lastCameraZ || pitch != lastCameraPitch || yaw != lastCameraYaw;
	//		lastCameraX = cameraPos.x;
	//		lastCameraY = cameraPos.y;
	//		lastCameraZ = cameraPos.z;
	//		lastCameraPitch = pitch;
	//		lastCameraYaw = yaw;
	//
	//		return needsTerrainUpdate;
	//	}
	//
	//	//  TODO: remove
	//	public void canvas_setNeedsTerrainUpdate(boolean needsUpdate) {
	//		needsTerrainUpdate = needsUpdate;
	//	}

}
