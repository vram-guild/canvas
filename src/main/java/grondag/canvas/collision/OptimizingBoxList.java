//package grondag.canvas.collision;
//
//import javax.annotation.Nullable;
//
//import com.google.common.collect.ImmutableList;
//
//import net.minecraft.util.math.Box;
//
//import grondag.fermion.varia.Useful;
//
//public class OptimizingBoxList implements Runnable
//{
//	// singleton is fine because called from a single thread
//	private static final OptimalBoxGenerator boxGen = new OptimalBoxGenerator();
//
//	private ImmutableList<Box> wrapped;
//	private @Nullable ISuperModelState modelState;
//
//	OptimizingBoxList(ImmutableList<Box> initialList, ISuperModelState modelState)
//	{
//		wrapped = initialList;
//		this.modelState = modelState;
//	}
//
//	protected ImmutableList<AxisAlignedBB> getList()
//	{
//		return wrapped;
//	}
//
//	@SuppressWarnings("null")
//	@Override
//	public void run()
//	{
//		final OptimalBoxGenerator generator = boxGen;
//		modelState.getShape().meshFactory().produceShapeQuads(modelState, generator);
//
//		//        generator.generateCalibrationOutput();
//
//		final int oldSize = wrapped.size();
//		final double oldVolume = Useful.volumeAABB(wrapped);
//		final double trueVolume = generator.prepare();
//		if(trueVolume == 0)
//			assert oldSize == 0 : "Fast collision box non-empty but detailed is empty";
//		else if(trueVolume != -1)
//		{
//			if(oldSize > ConfigXM.BLOCKS.collisionBoxBudget || Math.abs(trueVolume - oldVolume) > OptimalBoxGenerator.VOXEL_VOLUME * 2)
//				wrapped = generator.build();
//		}
//		//        if((CollisionBoxDispatcher.QUEUE.size() & 0xFF) == 0)
//		//            System.out.println("Queue depth = " + CollisionBoxDispatcher.QUEUE.size());
//
//		modelState = null;
//	}
//}
