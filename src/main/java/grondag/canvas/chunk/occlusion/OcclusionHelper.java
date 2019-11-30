package grondag.canvas.chunk.occlusion;

import java.util.EnumSet;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.util.math.Direction;

public class OcclusionHelper {
	public final EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
	public final IntArrayList list = new IntArrayList();
	public final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

	public OcclusionHelper clear() {
		faces.clear();
		list.clear();
		queue.clear();
		return this;
	}

	public static final ThreadLocal<OcclusionHelper> POOL = ThreadLocal.withInitial(OcclusionHelper::new);
}