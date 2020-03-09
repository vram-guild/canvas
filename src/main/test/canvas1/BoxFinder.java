package canvas1;

import java.util.Arrays;
import java.util.Random;

import com.google.common.base.Strings;
import io.netty.util.internal.ThreadLocalRandom;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

class BoxFinder {

	//	final Long2ObjectOpenHashMap<RectGroup> groups = new Long2ObjectOpenHashMap<>();

	static int rectKey(int x0, int y0, int x1, int y1) {
		return x0 | (y0 << 5) | (x1 << 10) | (y1 << 15);
	}

	static void printRect(int key) {
		final int x0 = key & 31;
		final int y0 = (key >> 5) & 31;
		final int x1 = (key >> 10) & 31;
		final int y1 = (key >> 15) & 31;

		final int x = x1 - x0 + 1;
		final int y = y1 - y0 + 1;
		final int a = x * y;
		System.out.println(String.format("%d x %d, area %d, (%d, %d) to (%d, %d)", x, y, a, x0, y0, x1, y1));
	}

	static long hashCoordinate (int x, int y) {
		return  1L << ((y << 3) | x);
	}

	static class AreaFinder {
		final Rect[] areas;

		final int areaCount;

		public AreaFinder() {
			final IntOpenHashSet keys = new IntOpenHashSet();

			keys.add(rectKey(0, 0, 15, 15));

			keys.add(rectKey(1, 0, 15, 15));
			keys.add(rectKey(0, 0, 14, 15));
			keys.add(rectKey(0, 1, 15, 15));
			keys.add(rectKey(0, 0, 15, 14));

			for (int dx = 14; dx >= 0; --dx) {
				for (int dy = 14; dy >= 0; --dy) {
					for (int x0 = 0; x0 < 15; x0++) {
						final int x1 = x0 + dx;

						if (x1 > 15) {
							break;
						}

						for (int y0 = 0; y0 < 15; y0++) {
							final int y1 = y0 + dy;

							if(y1 > 15) {
								break;
							}

							keys.add(rectKey(x0, y0, x1, y1));
						}
					}
				}
			}

			areaCount = keys.size();

			areas = new Rect[areaCount];

			int i = 0;

			for(final int k : keys) {
				areas[i++] = new Rect(k);
			}

			Arrays.sort(areas, (a, b) -> {
				final int result = Integer.compare(b.area, a.area);

				// within same area size, prefer more compact rectangles
				return result == 0 ? Integer.compare(a.edgeCount, b.edgeCount) : result;
			});
		}

		final long[] bits = new long[4];

		public void find(long[] bitsIn) {
			final long[] bits = this.bits;
			System.arraycopy(bitsIn, 0, bits, 0, 4);

			long hash = bitHash(bits);

			for(final Rect r : areas) {
				if (r.matchesHash(hash) && r.matches(bits)) {
					r.printShape();
					remove(r, bits);
					hash = bitHash(bits);

					if (hash == 0 || r.area < 4) {
						break;
					}
				}
			}
		}


	}

	@Test
	void test() {
		Rect rect = new Rect(rectKey(0, 0, 15, 15));
		assert(rect.bitHash == -1L);

		rect = new Rect(rectKey(0, 0, 0, 0));
		assert(rect.bitHash == 1);

		rect = new Rect(rectKey(4, 4, 5, 5));
		assert rect.bitHash == hashCoordinate(2, 2);

		rect = new Rect(rectKey(11, 11, 13, 13));
		assert rect.bitHash == (hashCoordinate(5, 5) | hashCoordinate(5, 6) | hashCoordinate(6, 5) | hashCoordinate(6, 6));




		//keys.forEach((int i) -> printRect(i));

		//System.out.println("Rect count = " + keys.size());

		//		final int[] bitCounts = new int[256];
		//
		//		keys.forEach((int i) -> {
		//			final Rect r = new Rect(i);
		//
		//			final long[] bits = r.bits;
		//
		//			for (int j = 0; j < 256; j++) {
		//				if ((bits[j >> 6] & (1L << (j & 63))) != 0) {
		//					bitCounts[j] += 1;
		//				}
		//			}
		//		});

		//		keys.forEach((int i) -> {
		//			final Rect r = new Rect(i);
		//			groups.computeIfAbsent(r.bitHash, RectGroup::new).add(r);
		//		});
		//
		//		groups.values().forEach(RectGroup::finish);
		//
		//		System.out.println("Group count = " + groups.size());

		//		System.out.print(Arrays.toString(bitCounts));

		//		final NodeBuilder builder = new NodeBuilder();
		//
		//		keys.forEach((int i) -> builder.add(new Rect(i)));
		//
		//		final Node node = builder.build();
		//
		final AreaFinder finder = new AreaFinder();
		final Sample sample = new Sample();

		final Random rand = ThreadLocalRandom.current();

		//final int limit = 256 + rand.nextInt(128);
		for(int i = 0; i < 512; i++) {
			final int x = rand.nextInt(16);
			final int y = rand.nextInt(16);
			sample.fill(x, y, x, y);
		}
		System.out.println("INPUT");
		printShape(sample.bits);

		System.out.println();
		System.out.println("OUTPUT");
		finder.find(sample.bits);
	}

	//	static class NodeBuilder {
	//		final ObjectArrayList<Rect> list = new ObjectArrayList<>();
	//
	//		void add(Rect r) {
	//			list.add(r);
	//		}
	//
	//		int[] bitCounts() {
	//			final int[] bitCounts = new int[256];
	//
	//			for (final Rect r : list) {
	//				final long[] bits = r.bits;
	//
	//				for (int j = 0; j < 256; j++) {
	//					if ((bits[j >> 6] & (1L << (j & 63))) != 0) {
	//						bitCounts[j] += 1;
	//					}
	//				}
	//			}
	//
	//			return bitCounts;
	//		}
	//
	//		Node build() {
	//			if (list.size() == 1) {
	//				return new EndNode(list.get(0));
	//			} else {
	//
	//				final long[] dependentBits = new long[4];
	//				final NodeBuilder independent = new NodeBuilder();
	//				final NodeBuilder dependent = new NodeBuilder();
	//
	//				int targetCount = (list.size() + 1) / 2;
	//
	//				while (targetCount > 0) {
	//					int best = -1;
	//					int bestCount = 0;
	//					final int[] bitCounts = bitCounts();
	//					for (int j = 0; j < 256; j++) {
	//						final int c = bitCounts[j];
	//
	//						if (c > bestCount && c <= targetCount) {
	//							best = j;
	//							bestCount = c;
	//						}
	//					}
	//
	//					if (best == -1) {
	//						if (dependent.list.isEmpty()) {
	//							System.out.println("boop");
	//						}
	//
	//						break;
	//					} else {
	//						dependentBits[best >> 6] |= (1L << (best & 63));
	//						targetCount -= bestCount;
	//					}
	//
	//					final ObjectListIterator<Rect> it = list.listIterator();
	//
	//					while (it.hasNext()) {
	//						final Rect r = it.next();
	//						final long[] bits = r.bits;
	//
	//						if ((bits[best >> 6] & (1L << (best & 63))) != 0) {
	//							dependent.add(r);
	//							it.remove();
	//						}
	//					}
	//				}
	//
	//				list.forEach(r -> independent.add(r));
	//
	//				assert !dependent.list.isEmpty();
	//				assert !independent.list.isEmpty();
	//
	//				return new BranchNode(independent.build(), dependent.build(), dependentBits);
	//			}
	//		}
	//	}
	//
	//	static abstract class Node {
	//		abstract Rect get(long bits[]);
	//	}
	//
	//	static class EndNode extends Node {
	//		final Rect rect;
	//
	//		EndNode(Rect rect) {
	//			this.rect = rect;
	//		}
	//
	//		@Override
	//		Rect get(long[] bits) {
	//			return rect.matches(bits) ? rect : null;
	//		}
	//	}
	//
	//	static class BranchNode extends Node {
	//		final Node independentNode;
	//
	//		final Node dependentNode;
	//
	//		final long[] dependencyBits;
	//
	//		BranchNode(Node independentNode, Node dependentNode, long[] dependencyBits) {
	//			this.independentNode = independentNode;
	//			this.dependentNode = dependentNode;
	//			this.dependencyBits = dependencyBits;
	//		}
	//
	//		@Override
	//		Rect get(long[] bits) {
	//			final long[] myBits = dependencyBits;
	//
	//			return match(myBits[0], bits[0])
	//					&& match(myBits[1], bits[1])
	//					&& match(myBits[2], bits[2])
	//					&& match(myBits[3], bits[3]) ? dependentNode.get(bits) : independentNode.get(bits);
	//		}
	//	}

	static class Sample {
		final long[] bits = new long[4];

		void fill(int x0, int y0, int x1, int y1) {
			for (int x = x0; x <= x1; x++) {
				for (int y = y0; y <= y1; y++) {
					final int key = (y << 4) | x;
					bits[key >> 6] |= (1L << (key & 63));
				}
			}
		}

		public void remove(Rect r) {
			BoxFinder.remove(r, bits);
		}

		void clear() {
			bits[0] = 0;
			bits[1] = 0;
			bits[2] = 0;
			bits[3] = 0;
		}


	}

	static class RectGroup {
		final long rectHash;

		final ObjectArrayList<Rect> rects = new ObjectArrayList<>();

		RectGroup(long rectHash) {
			this.rectHash = rectHash;
		}

		void add(Rect rect) {
			rects.add(rect);
		}

		void finish() {
			rects.sort((a, b) -> Integer.compare(b.area, a.area));
		}
	}

	static class Rect {
		final int rectKey;
		final int x0;
		final int y0;
		final int x1;
		final int y1;
		final int area;
		final int edgeCount;

		final long[] bits = new long[4];

		final long bitHash;

		boolean matches(long[] bits) {
			final long[] myBits = this.bits;

			return match(myBits[0], bits[0])
					&& match(myBits[1], bits[1])
					&& match(myBits[2], bits[2])
					&& match(myBits[3], bits[3]);
		}

		public boolean intersects(Rect other) {
			final long[] myBits = bits;
			final long[] otherBits = other.bits;
			return (myBits[0] & otherBits[0]) != 0
					|| (myBits[1] & otherBits[1]) != 0
					|| (myBits[2] & otherBits[2]) != 0
					|| (myBits[3] & otherBits[3]) != 0;
		}

		public boolean matchesHash(long hash) {
			return (bitHash & hash) == bitHash;
		}

		Rect(int rectKey) {
			this.rectKey = rectKey;
			x0 = rectKey & 31;
			y0 = (rectKey >> 5) & 31;
			x1 = (rectKey >> 10) & 31;
			y1 = (rectKey >> 15) & 31;

			final int x = x1 - x0 + 1;
			final int y = y1 - y0 + 1;
			area = x * y;
			edgeCount =  x +  y;

			populateBits();

			//			printShape();

			bitHash = bitHash(bits);

			//			printHash();
		}

		private void populateBits() {
			for (int x = x0; x <= x1; x++) {
				for (int y = y0; y <= y1; y++) {
					final int key = (y << 4) | x;
					bits[key >> 6] |= (1L << (key & 63));
				}
			}
		}

		public void printHash() {
			final String s = Strings.padStart(Long.toBinaryString(bitHash), 64, '0');
			System.out.println(s.substring(0, 8));
			System.out.println(s.substring(8, 16));
			System.out.println(s.substring(16, 24));
			System.out.println(s.substring(24, 32));
			System.out.println(s.substring(32, 40));
			System.out.println(s.substring(40, 48));
			System.out.println(s.substring(48, 56));
			System.out.println(s.substring(56, 64));
			System.out.println();
		}

		public void printShape() {
			BoxFinder.printShape(bits);
		}
	}

	public static void printShape(long[] bits) {
		String s = Strings.padStart(Long.toBinaryString(bits[3]), 64, '0');
		printSpaced(s.substring(0, 16));
		printSpaced(s.substring(16, 32));
		printSpaced(s.substring(32, 48));
		printSpaced(s.substring(48, 64));

		s = Strings.padStart(Long.toBinaryString(bits[2]), 64, '0');
		printSpaced(s.substring(0, 16));
		printSpaced(s.substring(16, 32));
		printSpaced(s.substring(32, 48));
		printSpaced(s.substring(48, 64));

		s = Strings.padStart(Long.toBinaryString(bits[1]), 64, '0');
		printSpaced(s.substring(0, 16));
		printSpaced(s.substring(16, 32));
		printSpaced(s.substring(32, 48));
		printSpaced(s.substring(48, 64));

		s = Strings.padStart(Long.toBinaryString(bits[0]), 64, '0');
		printSpaced(s.substring(0, 16));
		printSpaced(s.substring(16, 32));
		printSpaced(s.substring(32, 48));
		printSpaced(s.substring(48, 64));

		System.out.println();
	}

	private static void printSpaced(String s) {
		System.out.println(s.replace("0", "- ").replace("1", "X "));
	}
	public static void remove(Rect r, long[] bits) {
		final int x0 = r.x0;
		final int y0 = r.y0;
		final int x1 = r.x1;
		final int y1 = r.y1;

		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				final int key = (y << 4) | x;
				bits[key >> 6] &= ~(1L << (key & 63));
			}
		}
	}

	private static long bitHash(long bits[]) {
		long result = hashBits(bits[0], 0);
		result |= hashBits(bits[1], 1);
		result |= hashBits(bits[2], 2);
		result |= hashBits(bits[3], 3);

		return result;
	}

	private static long hashBits(long bits, int y) {
		long result = 0;

		long testMask = 0b11 | (0b11 << 16);

		long outputMask = hashCoordinate(0, y << 1);

		for (int x = 0; x < 8; x++) {
			if ((bits & testMask) != 0) {
				result |= outputMask;
			}

			testMask <<= 2;
			outputMask <<= 1;
		}

		testMask <<= 16;

		for (int x = 0; x < 8; x++) {
			if ((bits & testMask) != 0) {
				result |= outputMask;
			}

			testMask <<= 2;
			outputMask <<= 1;
		}

		//System.out.println(Long.toBinaryString(result));

		return result;
	}

	private static boolean match(long template, long candidate) {
		return (template & candidate) == template;
	}
}
