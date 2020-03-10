package canvas1;

import com.google.common.base.Strings;

public class TestUtils {

	public static void printShape(long[] bits, int index) {
		String s = Strings.padStart(Long.toBinaryString(bits[index + 3]), 64, '0');
		TestUtils.printSpaced(s.substring(0, 16));
		TestUtils.printSpaced(s.substring(16, 32));
		TestUtils.printSpaced(s.substring(32, 48));
		TestUtils.printSpaced(s.substring(48, 64));
	
		s = Strings.padStart(Long.toBinaryString(bits[index + 2]), 64, '0');
		TestUtils.printSpaced(s.substring(0, 16));
		TestUtils.printSpaced(s.substring(16, 32));
		TestUtils.printSpaced(s.substring(32, 48));
		TestUtils.printSpaced(s.substring(48, 64));
	
		s = Strings.padStart(Long.toBinaryString(bits[index + 1]), 64, '0');
		TestUtils.printSpaced(s.substring(0, 16));
		TestUtils.printSpaced(s.substring(16, 32));
		TestUtils.printSpaced(s.substring(32, 48));
		TestUtils.printSpaced(s.substring(48, 64));
	
		s = Strings.padStart(Long.toBinaryString(bits[index + 0]), 64, '0');
		TestUtils.printSpaced(s.substring(0, 16));
		TestUtils.printSpaced(s.substring(16, 32));
		TestUtils.printSpaced(s.substring(32, 48));
		TestUtils.printSpaced(s.substring(48, 64));
	
		System.out.println();
	}

	static void printSpaced(String s) {
		System.out.println(s.replace("0", "- ").replace("1", "X "));
	}

}
