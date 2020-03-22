package grondag.canvas.chunk.occlusion;

import com.google.common.base.Strings;

public class OcclusionBitPrinter {

	public static void printShape(long[] bits, int index) {
		String s = Strings.padStart(Long.toBinaryString(bits[index + 3]), 64, '0');
		OcclusionBitPrinter.printSpaced(s.substring(0, 16));
		OcclusionBitPrinter.printSpaced(s.substring(16, 32));
		OcclusionBitPrinter.printSpaced(s.substring(32, 48));
		OcclusionBitPrinter.printSpaced(s.substring(48, 64));

		s = Strings.padStart(Long.toBinaryString(bits[index + 2]), 64, '0');
		OcclusionBitPrinter.printSpaced(s.substring(0, 16));
		OcclusionBitPrinter.printSpaced(s.substring(16, 32));
		OcclusionBitPrinter.printSpaced(s.substring(32, 48));
		OcclusionBitPrinter.printSpaced(s.substring(48, 64));

		s = Strings.padStart(Long.toBinaryString(bits[index + 1]), 64, '0');
		OcclusionBitPrinter.printSpaced(s.substring(0, 16));
		OcclusionBitPrinter.printSpaced(s.substring(16, 32));
		OcclusionBitPrinter.printSpaced(s.substring(32, 48));
		OcclusionBitPrinter.printSpaced(s.substring(48, 64));

		s = Strings.padStart(Long.toBinaryString(bits[index + 0]), 64, '0');
		OcclusionBitPrinter.printSpaced(s.substring(0, 16));
		OcclusionBitPrinter.printSpaced(s.substring(16, 32));
		OcclusionBitPrinter.printSpaced(s.substring(32, 48));
		OcclusionBitPrinter.printSpaced(s.substring(48, 64));

		System.out.println();
	}

	static void printSpaced(String s) {
		System.out.println(s.replace("0", "- ").replace("1", "X "));
	}

	public static void printRegion(String header, long[] sample, int startIndex) {
		System.out.println(header);
		System.out.println();

		for(int z = 0; z < 16; z++) {
			System.out.println("Z = " + z);
			printShape(sample, startIndex + z * 4);
			System.out.println();
		}
	}

}
