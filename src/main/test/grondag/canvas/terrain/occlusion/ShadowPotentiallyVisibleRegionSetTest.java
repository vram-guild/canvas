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
		perfTest();
	}

	void correctnessTest() {
		set.setCameraChunkOriginAndClear(0, 0);

		//		final TestRegion xyz = new TestRegion(-512, 0, -512);
		//		final TestRegion xym = new TestRegion(-512, 0, 0);
		//		final TestRegion xyZ = new TestRegion(-512, 0, 512);
		//		final TestRegion myz = new TestRegion(0, 0, -512);
		//		final TestRegion mym = new TestRegion(0, 0, 0);
		//		final TestRegion myZ = new TestRegion(0, 0, 512);
		//		final TestRegion Xyz = new TestRegion(512, 0, -512);
		//		final TestRegion Xym = new TestRegion(512, 0, 0);
		//		final TestRegion XyZ = new TestRegion(512, 0, 512);
		//
		//		final TestRegion xmz = new TestRegion(-512, 128, -512);
		//		final TestRegion xmm = new TestRegion(-512, 128, 0);
		//		final TestRegion xmZ = new TestRegion(-512, 128, 512);
		//		final TestRegion mmz = new TestRegion(0, 128, -512);
		//		final TestRegion mmm = new TestRegion(0, 128, 0);
		//		final TestRegion mmZ = new TestRegion(0, 128, 512);
		//		final TestRegion Xmz = new TestRegion(512, 128, -512);
		//		final TestRegion Xmm = new TestRegion(512, 128, 0);
		//		final TestRegion XmZ = new TestRegion(512, 128, 512);
		//
		//		final TestRegion xYz = new TestRegion(-512, 240, -512);
		//		final TestRegion xYm = new TestRegion(-512, 240, 0);
		//		final TestRegion xYZ = new TestRegion(-512, 240, 512);
		//		final TestRegion mYz = new TestRegion(0, 240, -512);
		//		final TestRegion mYm = new TestRegion(0, 240, 0);
		//		final TestRegion mYZ = new TestRegion(0, 240, 512);
		//		final TestRegion XYz = new TestRegion(512, 240, -512);
		//		final TestRegion XYm = new TestRegion(512, 240, 0);
		//		final TestRegion XYZ = new TestRegion(512, 240, 512);
		//
		//		set.add(xyz);
		//		set.add(xym);
		//		set.add(xyZ);
		//		set.add(myz);
		//		set.add(mym);
		//		set.add(myZ);
		//		set.add(Xyz);
		//		set.add(Xym);
		//		set.add(XyZ);
		//
		//		set.add(xmz);
		//		set.add(xmm);
		//		set.add(xmZ);
		//		set.add(mmz);
		//		set.add(mmm);
		//		set.add(mmZ);
		//		set.add(Xmz);
		//		set.add(Xmm);
		//		set.add(XmZ);
		//
		//		set.add(xYz);
		//		set.add(xYm);
		//		set.add(xYZ);
		//		set.add(mYz);
		//		set.add(mYm);
		//		set.add(mYZ);
		//		set.add(XYz);
		//		set.add(XYm);
		//		set.add(XYZ);

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
		set.setLightVectorAndRestartSlowly(a, b, c);

		TestRegion r = set.next();
		float lastDist = Float.MIN_VALUE;

		while (r != null) {
			float dist = distanceToPlane(r.origin);
			// regions should be same distance or going farther from skylight

			if (dist < lastDist) {
				System.out.println(r.origin.toShortString() + " dist=" + dist + "   FAIL");
			} else {
				if (dist > lastDist) {
					System.out.println("step size: " + (dist - lastDist));
				}

				//System.out.println(r.origin.toShortString() + " dist=" + dist);
			}

			//assert dist >= lastDist;
			//assert lastDist == Float.MIN_VALUE || Math.abs(dist - lastDist) <= 12;
			lastDist = dist;
			r = set.next();
		}
	}

	@SuppressWarnings("unused")
	private void perfTest() {
		ShadowPotentiallyVisibleRegionSet<TestRegion> set = new ShadowPotentiallyVisibleRegionSet<>(new TestRegion[RenderRegionIndexer.PADDED_REGION_INDEX_COUNT]);
		set.setCameraChunkOriginAndClear(0, 0);

		long ns = System.nanoTime();

		for (int i = 0; i < 1000; ++i) {
			set.setLightVectorAndRestart(0, 0.8f, 0.6f);
		}

		System.out.println("Sweep Method: " + (System.nanoTime() - ns) / 1000 + " ns/run");

		set = new ShadowPotentiallyVisibleRegionSet<>(new TestRegion[RenderRegionIndexer.PADDED_REGION_INDEX_COUNT]);
		set.setCameraChunkOriginAndClear(0, 0);

		ns = System.nanoTime();

		for (int i = 0; i < 1000; ++i) {
			set.setLightVectorAndRestartSlowly(0, 0.8f, 0.6f);
		}

		System.out.println("Naive Method: " + (System.nanoTime() - ns) / 1000 + " ns/run");
	}
}
