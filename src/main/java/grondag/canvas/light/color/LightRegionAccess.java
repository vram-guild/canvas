package grondag.canvas.light.color;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface LightRegionAccess {
	LightRegionAccess EMPTY = new Empty();

	void checkBlock(BlockPos pos, BlockState blockState);
	boolean isClosed();

	class Empty implements LightRegionAccess {
		@Override
		public void checkBlock(BlockPos pos, BlockState blockState) { }

		@Override
		public boolean isClosed() {
			return true;
		}
	}
}
