package grondag.canvas.terrain.occlusion;

import org.junit.jupiter.api.Test;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.terrain.region.RenderRegionIndexer;

class ShadowPotentiallyVisibleRegionSetTest {
	private static record TestRegion(BlockPos origin) implements ShadowPotentiallyVisibleRegion {
		TestRegion(int x, int y, int z) {
			this(new BlockPos(x, y, z));
		}
	}

	// plane equation
	float a, b, c, d;
	final ShadowPotentiallyVisibleRegionSet<TestRegion> set = new ShadowPotentiallyVisibleRegionSet<>(new TestRegion[RenderRegionIndexer.PADDED_REGION_INDEX_COUNT]);

	// assume a, b and c are normalized
	float distanceToPlane(int x, int y, int z) {
		return Math.abs(a * x + b * y + c * z + d);
	}

	float distanceToPlane(BlockPos pos) {
		return distanceToPlane(pos.getX(), pos.getY(), pos.getZ());
	}

	@Test
	void test() {
		correctnessTest();
	}

	void correctnessTest() {
		set.setCameraChunkOriginAndClear(0, 0);

		for (int x = -512; x <= 512; x += 16) {
			for (int z = -512; z <= 512; z += 16) {
				for (int y = 0; y <= 240; y += 16) {
					set.add(new TestRegion(x, y, z));
				}
			}
		}

		a = 0f;
		b = 0.8f;
		c = (float) Math.sqrt(1f - b * b);
		d = -10000f;

		testDirection();

		// sweep up - light is down
		//b = -b;
		//testDirection();
	}

	private void testDirection() {
		set.setLightVectorAndRestart(a, b, c);

		TestRegion r = set.next();

		while (r != null) {
			System.out.println(r.origin.toString());
			r = set.next();
		}
	}
}
