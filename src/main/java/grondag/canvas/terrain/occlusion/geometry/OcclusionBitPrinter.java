/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.terrain.occlusion.geometry;

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

		for (int z = 0; z < 16; z++) {
			System.out.println("Z = " + z);
			printShape(sample, startIndex + z * 4);
			System.out.println();
		}
	}
}
