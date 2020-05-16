//package canvas1;
//
//import java.util.Random;
//
//import io.netty.util.internal.ThreadLocalRandom;
//import org.junit.jupiter.api.Test;
//
//import grondag.canvas.chunk.occlusion.OcclusionBounds;
//
//class OcclusionBoundsTest {
//
//	@Test
//	void test() {
//		final Random r = ThreadLocalRandom.current();
//
//		for (int i = 0; i < 500; i++) {
//
//			final int face = r.nextInt(6);
//			final int depth = r.nextInt(16);
//			final int u0 = r.nextInt(16);
//			final int v0 = r.nextInt(16);
//			final int u1 = Math.min(16, u0 + r.nextInt(15) + 1);
//			final int v1 = Math.min(16, v0 + r.nextInt(15) + 1);
//
//			final int bounds = OcclusionBounds.pack(face, depth, u0, v0, u1, v1);
//
//			assert OcclusionBounds.face(bounds) ==  face;
//			assert OcclusionBounds.depth(bounds) ==  depth;
//			assert OcclusionBounds.u0(bounds) == u0;
//			assert OcclusionBounds.v0(bounds) ==  v0;
//			assert OcclusionBounds.u1(bounds) ==  u1;
//			assert OcclusionBounds.v1(bounds) ==  v1;
//			assert OcclusionBounds.size(bounds) ==  (v1 - v0) * (u1 - u0);
//		}
//	}
//}
