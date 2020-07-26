package grondag.canvas.apiimpl.util;

import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

public class FaceConstants {

	public final static int NORTH_INDEX = ModelHelper.toFaceIndex(Direction.NORTH);
	public final static int SOUTH_INDEX = ModelHelper.toFaceIndex(Direction.SOUTH);
	public final static int EAST_INDEX = ModelHelper.toFaceIndex(Direction.EAST);
	public final static int WEST_INDEX = ModelHelper.toFaceIndex(Direction.WEST);
	public final static int UP_INDEX = ModelHelper.toFaceIndex(Direction.UP);
	public final static int DOWN_INDEX = ModelHelper.toFaceIndex(Direction.DOWN);

}
