/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.terrain.occlusion.region;

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

	public static void printSpaced(String s) {
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
